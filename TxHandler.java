import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    // Scrooge's private record of the public ledger
    private UTXOPool scroogePool;

    public TxHandler(UTXOPool utxoPool) {
        //(declare a new ledger - done as a private attribute of class TxHandler)
        //Assign a deep copy of utxoPool to scroogePool
        this.scroogePool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        //(1)return false if any of tx.input is not in scroogePool
        //loop through all inputs
        ArrayList<Transaction.Input> all_inputs = tx.getInputs();
        Set<UTXO> seen;
        double input_sum = 0;
        for (int i = 0; i < tx.numInputs() ; i++) {
            //UTXO constructor
            UTXO inUTXO = new UTXO(all_inputs.get(i).prevTxHash, all_inputs.get(i).outputIndex);
            input_sum+= this.scroogePool.getTxOutput(inUTXO).value;

            //retrieve the public key from the output of this UTXO
            Transaction.Output out = this.scroogePool.getTxOutput(inUTXO);
            if (out == null){
                return false; //means the input UTXO is not in the pool
            }
            //(2) check signature on previous transaction matches the public key of the sender of this transaction
            if(!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), all_inputs.get(i).signature){
                return false;
            }
            //(3) check if UTXOs in tx is duplicated
            int count_before = seen.size();
            seen.add(inUTXO);
            if (seen.size() == count_before){
                return false;
            }
        }
        //finished checking inputs, now check outputs
        ArrayList<Transaction.Output> all_outputs = tx.getOutputs();
        double output_sum = 0;
        for (int i =  0; i< tx.numOutputs(); i++){
            //(4) non-negative values
            if (all_outputs.get(i).value < 0 ) {
                return false;
            }
            output_sum += all_outputs.get(i).value;
        }
        //(5)the sum of {@code tx}s input values is greater than or equal to the sum of its output
        return (input_sum >= output_sum);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    }

}
