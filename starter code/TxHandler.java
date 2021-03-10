import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is utxoPool. This should make a defensive copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	private UTXOPool utxoPool;
	private HashSet<UTXO> claimedOutputs = new HashSet<UTXO>();

	public TxHandler(UTXOPool utxoPool) {
		this.utxoPool = new UTXOPool(utxoPool);
	}

	/* Returns true if
	 * (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid,
	 * (3) no UTXO is claimed multiple times by tx,
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of
	        its output values;
	   and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		ArrayList<Transaction.Input> myInputs = tx.getInputs();
		HashSet<UTXO> claimedOutputs = new HashSet<UTXO>();
		double inputSum = 0;
		double outputSum = 0;

		// check tx inputs to be in UTXO Pool of outputs
		for ( int i = 0; i < myInputs.size(); i++){
			Transaction.Input myInput = myInputs.get(i);
			UTXO myUTXO = new UTXO( myInput.prevTxHash, myInput.outputIndex);

			if ( this.utxoPool.contains(myUTXO) == false || claimedOutputs.contains(myUTXO) ){
				return false;
			}

			// check tx input signatures
			if(!this.utxoPool.getTxOutput(myUTXO).address.verifySignature(tx.getRawDataToSign(i), myInput.signature)) { // (2)
				return false;
			}

			//increment total inputs
			inputSum += this.utxoPool.getTxOutput(myUTXO).value;

			//avoid double-counting
			claimedOutputs.add(myUTXO);
		}

		//calculate total outputs
		for ( int i = 0; i < tx.numOutputs(); i++){

			double tempVal = tx.getOutput(i).value;

			if (tx.getOutput(i).value >= 0){
				outputSum += tempVal;

			} else {
				return false;
			}

		}

		// ensure tx input and outputs are net-positive
		if ( outputSum > inputSum){
			return false;
		}

		// remove tx inputs from UTXO pool
		this.claimedOutputs = claimedOutputs;
		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness,
	 * returning a mutually valid array of accepted transactions,
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> ans = new ArrayList<Transaction>();

		//iterate through transactions
		for ( int i = 0; i < possibleTxs.length; i++){
			this.claimedOutputs.clear();

			if ( isValidTx(possibleTxs[i])){
				ans.add(possibleTxs[i]);

				//remove used outputs from UTXOPool
				for (UTXO out : this.claimedOutputs){
					this.utxoPool.removeUTXO(out);
				}

				//update UTXOPool with new outputs
				for ( int j = 0; j < possibleTxs[i].numOutputs(); j++){
					UTXO myUTXO = new UTXO(possibleTxs[i].getHash(), j);
					this.utxoPool.addUTXO(myUTXO, possibleTxs[i].getOutput(j));
				}

			}

		}

		return ans.toArray( new Transaction[ans.size()]);
	}

}