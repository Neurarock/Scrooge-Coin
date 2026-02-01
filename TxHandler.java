import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
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

    //if fee is negative transaction is invalid
    public double fee(Transaction tx) {
        double output_sum = 0;
        for (int i =  0; i< tx.numOutputs(); i++){
            output_sum += tx.getOutput(i).value;
        }
        double input_sum = 0;
        for (int i =  0; i< tx.numInputs(); i++){
            UTXO inUTXO = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
            input_sum+= this.scroogePool.getTxOutput(inUTXO).value;
        }
        return (input_sum - output_sum);
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
        Set<UTXO> seen = new HashSet<>();
        double input_sum = 0;
        for (int i = 0; i < tx.numInputs() ; i++) {
            //UTXO constructor
            UTXO inUTXO = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
            input_sum+= this.scroogePool.getTxOutput(inUTXO).value;

            //retrieve the public key from the output of this UTXO
            Transaction.Output out = this.scroogePool.getTxOutput(inUTXO);
            if (out == null){
                return false; //means the input UTXO is not in the pool
            }
            //(2) check signature on previous transaction matches the public key of the sender of this transaction
            if(!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), tx.getInput(i).signature ) ){
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
        double output_sum = 0;
        for (int i =  0; i< tx.numOutputs(); i++){
            //(4) non-negative values
            if (tx.getOutput(i).value < 0 ) {
                return false;
            }
            output_sum += tx.getOutput(i).value;
        }
        //(5)the sum of {@code tx}s input values is greater than or equal to the sum of its output
        return (input_sum >= output_sum);
    }

    //helper to change the status of the UTXO Pool (Assume validity already checked)
    public void spendUTXO(Transaction tx) {
        //spend inputs
        for (int i = 0; i < tx.numInputs() ; i++) {
            UTXO inUTXO = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
            this.scroogePool.removeUTXO(inUTXO);//spend all of these UTXOs
        }
        //add outputs to Pool
        for (int i = 0; i < tx.numOutputs() ; i++) {
            UTXO leftOverUTXO = new UTXO(tx.getHash(), i);
            this.scroogePool.addUTXO(leftOverUTXO, tx.getOutput(i));//add new ones to pool
        }
    }



    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // Extract a list of valid(by itself) transactions (correct)
        ArrayList<Transaction> acceptedTx = new ArrayList<>();
        for (Transaction t : possibleTxs){
            if (!this.isValidTx(t)) {
                continue;
            }
            //if it is valid, spend the coins and add to acceptedTx
            //spend in UTXO logic
            spendUTXO(t);
            acceptedTx.add(t);
        }
        return acceptedTx.toArray(new Transaction[acceptedTx.size()]);
    }

    /*
     * For maximum fee
     * Recursively search through all possible combinations
     * of transactions and choose the one with the highest fee
     */

}
