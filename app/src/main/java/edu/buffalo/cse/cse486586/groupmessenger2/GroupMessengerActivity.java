
package edu.buffalo.cse.cse486586.groupmessenger2;

        import android.app.Activity;
        import android.content.ContentValues;
        import android.content.Context;
        import android.net.Uri;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.telephony.TelephonyManager;
        import android.text.method.ScrollingMovementMethod;
        import android.util.Log;
        import android.view.Menu;
        import android.view.View;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.TextView;

        import java.io.DataOutputStream;
        import java.io.IOException;
        import java.net.InetAddress;
        import java.net.ServerSocket;
        import java.net.Socket;
        import java.net.SocketTimeoutException;
        import java.net.UnknownHostException;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collections;
        import java.util.Comparator;
        import java.util.HashMap;
        import java.util.Iterator;
        import java.util.Map;
        import java.util.PriorityQueue;
        import java.io.*;
    /**
     * GroupMessengerActivity is the main Activity for the assignment.
     *
     * @author stevko
     *
     */

    public class GroupMessengerActivity extends Activity {
        static final String TAG = GroupMessengerActivity.class.getSimpleName();
        static final String REMOTE_PORT0 = "11108";
        static final String REMOTE_PORT1 = "11112";
        static final String REMOTE_PORT2 = "11116";
        static final String REMOTE_PORT3 = "11120";
        static final String REMOTE_PORT4 = "11124";
        static final int SERVER_PORT = 10000;
        int msgnum = 0;         //for storing keys
        int propose_num = 0;    //sequence number proposed by processes
        int agreed_num = 0;     //max agreed sequence number for a message
        int unique_msg = 0;     //msg_id
        int alive_process = 5;
        String failPort = "";
        boolean failed = false;
        static ArrayList<String> ports = new ArrayList<String>(Arrays.asList(REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4));
        Comparator<messages> comparator = new seqnumComparator();
        PriorityQueue<messages> ReceivedMsgs = new PriorityQueue<messages>(25, comparator);
        HashMap<Integer, ArrayList<Integer>> msgcount = new HashMap<Integer, ArrayList<Integer>>();
        HashMap<Integer, messages> msgsent = new HashMap<Integer, messages>();
        String myPort;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_group_messenger);

            TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            myPort = String.valueOf((Integer.parseInt(portStr) * 2));

            /*
             * TODO: Use the TextView to display your messages. Though there is no grading component
             * on how you display the messages, if you implement it, it'll make your debugging easier.
             */


            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            } catch (IOException e) {
                Log.e(TAG, "Can't create a ServerSocket");
                return;
            }


            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.setMovementMethod(new ScrollingMovementMethod());

            /*
             * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
             * OnPTestClickListener demonstrates how to access a ContentProvider.
             */
            findViewById(R.id.button1).setOnClickListener(
                    new OnPTestClickListener(tv, getContentResolver()));

            /*
             * TODO: You need to register and implement an OnClickListener for the "Send" button.
             * In your implementation you need to get the message from the input box (EditText)
             * and send it to other AVDs.
             */
            Button b = (Button) findViewById(R.id.button4);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText et = (EditText) findViewById(R.id.editText1);
                    String msg = et.getText().toString() + "\n";
                    messages m = new messages(unique_msg, msg, myPort, propose_num, 0, true, false);
                    unique_msg++;
                    et.setText(""); // This is one way to reset the input box.
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\t" + msg); // This is one way to display a string.
                    Log.i(TAG,"Calling ClientTask from onCreate");

                    msgsent.put(m.getMsg_id(), m);

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, m);
                }
            });
        }


        class seqnumComparator implements Comparator<messages> {

            @Override
            public int compare(messages obj1, messages obj2) {
                if (obj1.seq_num < obj2.seq_num)
                    return -1;
                else if (obj1.seq_num > obj2.seq_num)
                    return 1;
                else {
                    if (Integer.parseInt(obj1.portnum) < Integer.parseInt(obj2.portnum)) {
                        return -1;
                    } else if (Integer.parseInt(obj1.portnum) > Integer.parseInt(obj2.portnum)) {
                        return 1;
                    }
                }
                return 0;
            }
        }

        private class ServerTask extends AsyncTask<ServerSocket, messages, Void> {

            @Override
            protected Void doInBackground(ServerSocket... sockets) {
                ServerSocket serverSocket1 = sockets[0];

                while (true) {
                    try {
                        Socket socket1 = serverSocket1.accept();
                        Log.i(TAG, "Connection Accepted by server");
                        ObjectInputStream in = new ObjectInputStream(socket1.getInputStream());
                        messages msg1 = (messages) in.readObject();
                        Log.i(TAG, "Received msg from client " + msg1.portnum);
//
                        DataOutputStream d = new DataOutputStream(socket1.getOutputStream());
                        d.writeUTF("ACK");
//                    BufferedReader br = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
//                    String message = br.readLine();
                        ArrayList<Integer> new_arr ;

                        int flag1 = msg1.flag;
                        System.out.println(flag1);

                        if(flag1 == 0)
                        {
                            messages msg3 = new messages(msg1.getMsg_id(), msg1.getMsg_text(), msg1.portnum, msg1.seq_num, msg1.flag, msg1.multi_uni, msg1.deliverable);
                            ReceivedMsgs.add(msg3);

                            if (agreed_num >= propose_num)
                            {
                                propose_num = agreed_num + 1;
                            }

                            else
                            {
                                propose_num = propose_num + 1;
                            }

                            //send_port = msg1.portnum;
                            msg1.seq_num = propose_num;
                            //msg1.portnum = myPort;
                            msg1.multi_uni=false;
//                       messages m2 = new messages(Integer.parseInt(msgparts[0]), msgparts[1], msgparts[2], propose_num, 1, false, Boolean.parseBoolean(msgparts[6]));
//                        String sendmsg = m2.toString();
                            publishProgress(msg1);
                        }

                        else if (flag1 == 1)
                        {
                            int mid = msg1.getMsg_id();
                            int propose_num1 = msg1.seq_num;
                            //Log.i(TAG, "Received uni-cast from client " + msg1.portnum + " with proposal " + msg1.seq_num +" for msg "+ msg1.getMsg_text());

                            if (msgcount.containsKey(mid))
                            {
                                new_arr = msgcount.get(mid);
                                new_arr.add(propose_num1);
                                msgcount.put(mid, new_arr);
                            }

                            else
                            {
                                new_arr = new ArrayList<Integer>();
                                new_arr.add(propose_num1);
                                msgcount.put(mid, new_arr);
                            }


                            if (new_arr.size() == alive_process)
                            {
                                agreed_num = Collections.max(new_arr);
                                messages m2 = new messages(mid, msg1.getMsg_text(), myPort, agreed_num, 1, true, true);
                                //send_port = msgparts[2];
                                //String msgg = m2.toString();
                                //String command = "send";
                                //String sendmsg = m2.toString();
                                //propose_num = agreed_num;
                                msgsent.remove(mid);
                                Log.i(TAG, "Sending final broadcast from Server "+m2.getMsg_text() + " with seq num "+ m2.seq_num);

                                publishProgress(m2);
                            }
                        }

                        else if (flag1 == 2)
                        {
                            //String command = "receive";
                            messages head = msg1;

                            Iterator it2 = ReceivedMsgs.iterator();

//                        while(it2.hasNext())
//                        {
//                            System.out.println(it2.next());
//                        }

                            while (it2.hasNext())
                            {
//                        System.out.println("In second while");
                                messages m = (messages) it2.next();
                                System.out.println(m.getMsg_id() + " already in queue " + m.portnum + m.deliverable);
                                System.out.println(head.getMsg_id() + " value of head " + head.portnum + head.deliverable);

                                if (m.msg_id == head.msg_id && Integer.parseInt(m.portnum) == Integer.parseInt(head.portnum))
                                {
                                    System.out.println("In if statement");
                                    ReceivedMsgs.remove(m);
                                    break;
                                }
                            }
                            //ReceivedMsgs.remove(head);
                            ReceivedMsgs.add(head);

                            messages head1 = ReceivedMsgs.peek();
                            while (head1!=null)
                            {
//                            System.out.println(head1.getMsg_id()+" "+head1.portnum+" "+head1.deliverable);
                                if(head1.deliverable==true)
                                {
                                    System.out.println("Writing messages to content provider");
                                    messages m3 = new messages(head1.msg_id, head1.msg_text, head1.portnum, head1.seq_num, head1.flag, head1.multi_uni, head1.deliverable);
                                    ReceivedMsgs.poll();
                                    head1 = ReceivedMsgs.peek();
                                    publishProgress(m3);
                                }
                                else {
                                    break;
                                }
                            }

                        }

                        else if (flag1 == 3)
                        {
                            Log.i(TAG, "Remove failPort" + msg1.portnum);

//                            if(ports.contains(msg1.portnum))
//                            {
//                                ports.remove(msg1.portnum);
//                            }

                            alive_process = 4;

                            Iterator it = ReceivedMsgs.iterator();

                            System.out.println("FailedPort ---->" + msg1.portnum);

                            while (it.hasNext())
                            {
                                messages m = (messages) it.next();
                                System.out.println(m.getMsg_id() + " already in queue " + m.portnum + m.deliverable);
                                if(m.portnum.equals(msg1.portnum))
                                {
                                    Log.i(TAG,"Removing failPort messages");
                                    ReceivedMsgs.remove(m);
                                }
                            }


                            for (Map.Entry<Integer, ArrayList<Integer>> i : msgcount.entrySet())
                            {
                                ArrayList<Integer> arr = i.getValue();
                                Integer k = i.getKey();
                                messages m2 = msgsent.get(k);


                                if(arr.size()==4 && m2!=null){

                                    msgsent.remove(k);
                                    Log.i(TAG,"Sending messages with 4 proposals");
                                    Integer num = Collections.max(arr);
                                    m2.seq_num = num;
                                    m2.flag = 1;
                                    m2.multi_uni = true;
                                    m2.deliverable = true;
                                    publishProgress(m2);
                                }


                            }

                        }


                        //socket1.close();
                    } catch (IOException e) {
                        e.printStackTrace();


                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }


            protected void onProgressUpdate(messages... msgs) {
                /*
                 * The following code displays what is received in doInBackground().
                 */
//            String strReceived = strings[0].trim();
//            String[] msgparts = strReceived.split(":");
                messages m2 = msgs[0];

                int flag1 = m2.flag;

                if (flag1 == 0)
                {
                    //send_port = msgparts[2];
                    //Log.i(TAG, "Sending proposal from Server " + myPort + " to server " + send_port);
                    m2.flag = 1;
                    //messages m3 = new messages(Integer.parseInt(msgparts[0]), msgparts[1], myPort, propose_num, 1, false, Boolean.parseBoolean(msgparts[6]));
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, m2);
                }


                else if (flag1 == 1)
                {
                    m2.flag = 2;
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, m2);
                }


                else if (flag1 == 2)
                {
                    System.out.println("Message written in content provider");
                    TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                    /*referenced from PA2 A specification doc*/
                    ContentValues keyValueToInsert = new ContentValues();
                    keyValueToInsert.put("key", String.valueOf(msgnum));
                    keyValueToInsert.put("value", m2.getMsg_text());
//                            System.out.println("Saving .....");
                    Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
                    getContentResolver().insert(uri, keyValueToInsert);
                    msgnum++;
                    remoteTextView.append(m2.getMsg_text() + "\t\n");

                }



            }
        }

        private class ClientTask extends AsyncTask<messages, messages, Void> {

            @Override
            protected Void doInBackground(messages... msgs) {
                messages obj = msgs[0];
                String msgtosend = obj.getMsg_text();
                msgtosend = msgtosend.replaceAll("\\n", "");
                //String parts[] = msgtosend.split(":");
                //System.out.println(parts[5]);

                if (obj.multi_uni) //multi-cast message
                {
//                    System.out.println("In Broadcast operation");
                    for (String remotePort : ports) {
                        try {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));

                            //socket.setSoTimeout(500);
                            /*
                             * TODO: Fill in your client code that sends out a message.
                             */
                            Log.i(TAG, "Broadcasting message " + msgtosend);

                            OutputStream outputStream = socket.getOutputStream();
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                            //System.out.println("Sending messages to the ServerSocket");
                            objectOutputStream.writeObject(obj);

                            DataInputStream ds = new DataInputStream(socket.getInputStream());
                            String ack = ds.readUTF();
//                            Log.i(TAG, "Acknowledgement : " + ack);

                        } catch (UnknownHostException e) {
                            Log.e(TAG, "ClientTask UnknownHostException");
                        } catch (SocketTimeoutException e) {
                            Log.e(TAG, "ClientTask socket timeout exception");
                        } catch (IOException e) {
                            Log.e(TAG, "ClientTask socket IOException at multicast");
                            failPort = remotePort;
                            alive_process = 4;
                            System.out.println("Exception caught at : "+failPort);
                            continue;
                        }

                    }

                    if (failPort.length()>0 && ports.contains(failPort))
                    {
                        if(failed!=true) {
                            publishProgress(obj);
                        }
                    }
                }


                else if (!obj.multi_uni) //uni-cast message
                {
                    try {
                        String remotePort = obj.portnum;
                        Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));

                        Log.i(TAG, "Unicasting message to " + remotePort + " with msg "+ msgtosend);


                        OutputStream outputStream = socket2.getOutputStream();
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                        //System.out.println("Sending messages to the ServerSocket");
                        objectOutputStream.writeObject(obj);

//                        DataOutputStream ds = new DataOutputStream(socket2.getOutputStream());
//                        ds.write(msgtosend.getBytes("UTF-8"));
//                        ds.close();
                        //socket2.close();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException at unicast");
                    }
                }

                return null;
            }

            protected void onProgressUpdate(messages... msgobj)
            {
                messages m3 = msgobj[0];
                messages m4 = new messages(m3.getMsg_id(), m3.getMsg_text(), failPort, m3.seq_num, 3, true, m3.deliverable);
                Log.i(TAG,"Sending out failPort message");
                failed = true;
                //ports.remove(failPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, m4);
            }
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
            return true;
        }
    }





