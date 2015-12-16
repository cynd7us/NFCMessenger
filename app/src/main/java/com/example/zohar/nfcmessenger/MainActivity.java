package com.example.zohar.nfcmessenger;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        NfcAdapter.OnNdefPushCompleteCallback, NfcAdapter.CreateNdefMessageCallback {

    private ArrayList<String> messagesToSendQueue = new ArrayList<>();
    private ArrayList<String> messagesToReceiveQueue = new ArrayList<>();

    private EditText addMessageInput;
    private TextView receivedMessageOutput;
    private TextView messagesToSend;

    private NfcAdapter deviceNfcAdapter;

    /**
     * Checks if there is any content filled to send as a message
     *
     * @param  view the view to check for content filled
     * */
    public void addMessage(View view) {
        String message = addMessageInput.getText().toString();
        messagesToSendQueue.add(message);
        addMessageInput.setText(null);

        Toast.makeText(this, "Message added", Toast.LENGTH_SHORT).show();

        updateTextViews();
    }

    /**
     * Fills the text views in the current view with all of
     * messages to send array and messages to receive array
     *
     */
    private void updateTextViews() {
        messagesToSend.setText("Messages to send: \n");
        for(int counter = 0; counter < messagesToSendQueue.size(); counter++) {
            messagesToSend.append(messagesToSendQueue.get(counter) + "\n");
        }

        receivedMessageOutput.setText("Messages to receive: \n");
        for(int counter = 0; counter < messagesToReceiveQueue.size(); counter++) {
            receivedMessageOutput.append(messagesToReceiveQueue.get(counter) + "\n");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putStringArrayList("messagesToSend", messagesToSendQueue);
        savedInstanceState.putStringArrayList("messagesToReceive", messagesToReceiveQueue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(deviceNfcAdapter != null) {
            // Refer back to createNdefMessage to send
            deviceNfcAdapter.setNdefPushMessageCallback(this, this);

            // This will be called if message sent successfully
            deviceNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        } else {
            Toast.makeText(this, "NFC not available on your device.",
                    Toast.LENGTH_LONG).show();
        }

        addMessageInput = (EditText) findViewById(R.id.txtBoxAddMessage);
        messagesToSend = (TextView) findViewById(R.id.txtMessageToSend);
        receivedMessageOutput = (TextView) findViewById(R.id.txtMessagesReceived);
        Button addMessage = (Button) findViewById(R.id.buttonAddMessage);

        updateTextViews();

        if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            handleNfcIntent(getIntent());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTextViews();
        handleNfcIntent(getIntent());
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        // this is called when the system detected ndef message
        messagesToSendQueue.clear();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        // this will be called when another device is detected
        NdefMessage record = null;
        if(messagesToSendQueue.size() > 0) {
            NdefRecord[] recordsToSend = createRecords();
            record = new NdefMessage(recordsToSend);
        }
        return record;
    }

    /**
     * Creates records to send to another device if detected
     * those records are about to send to another device
     *
     * @return NdefRecord[] - array of records to attach
     */
    public NdefRecord[] createRecords() {
        NdefRecord[] records = new NdefRecord[messagesToSendQueue.size()];

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            for (int counter = 0; counter < messagesToSendQueue.size(); counter++) {
                byte[] payload = messagesToSendQueue.get(counter).getBytes
                        (Charset.forName("UTF-8"));

                NdefRecord record = new NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN,
                        NdefRecord.RTD_TEXT,
                        new byte[0],
                        payload
                );

                records[counter] = record;
            }
        } else {
            for(int counter = 0; counter < messagesToSendQueue.size(); counter++) {
                byte[] payload = messagesToSendQueue.get(counter).
                        getBytes(Charset.forName("UTF-8"));
                NdefRecord record = NdefRecord.createMime("text/plain", payload);
                records[counter] = record;
            }
        }

        records[messagesToSendQueue.size()] =
                NdefRecord.createApplicationRecord(getPackageName());
        return records;
    }

    /**
     * Method to send the messages over another device
     *
     * @param NfcIntent - NFC intent to pass data
     *
     */
    private void handleNfcIntent(Intent NfcIntent) {
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(NfcIntent.getAction())) {
            Parcelable[] receivedArray =
                    NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(receivedArray != null) {
                messagesToReceiveQueue.clear();
                NdefMessage receivedMessage = (NdefMessage) receivedArray[0];
                NdefRecord[] linkRecords = receivedMessage.getRecords();

                for(NdefRecord record : linkRecords) {
                    String message = new String(record.getPayload());
                    if(!message.equals(getPackageName())) {
                        messagesToReceiveQueue.add(message);
                    }
                }

                Toast.makeText(this, "Total " + messagesToReceiveQueue.size()
                + "Messages received", Toast.LENGTH_LONG).show();

                updateTextViews();
            } else {
                Toast.makeText(this, "Received Blank", Toast.LENGTH_LONG).show();
            }
        }
    }
}
