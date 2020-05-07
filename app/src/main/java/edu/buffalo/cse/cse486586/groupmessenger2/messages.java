package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

public class messages implements Serializable {

    int msg_id;                             //uniquely identify a message
    String msg_text;                        //message text
    public String portnum;                  //Port number from which message was sent i.e. process id
    public int seq_num;                     //Sequence number suggested by process for a message
    public int flag;                        //to identify what action to take
    public boolean multi_uni;               //identify to multi-cast or uni-cast the message
    public boolean deliverable;             //identify if message is ready to deliver

    public messages(int msg_id, String msg_text, String portnum, int seq_num, int flag, boolean multi_uni, boolean deliverable)
    {
        this.msg_id = msg_id;
        this.msg_text = msg_text;
        this.portnum = portnum;
        this.seq_num = seq_num;
        this.flag = flag;
        this.multi_uni = multi_uni;
        this.deliverable = deliverable;
    }


//    public String toString()
//    {
//        return this.msg_id+":"+this.msg_text+":"+this.portnum+":"+this.seq_num+":"+this.flag+":"+this.multi_uni+":"+this.deliverable;
//    }

    public String getMsg_text()
    {
        return msg_text;
    }

    public int getMsg_id()
    {
        return msg_id;
    }



//    @Override
//    public boolean equals(Object obj)
//    {
//        if(obj instanceof messages) {
//            messages msg = (messages)obj;
//                return (this.msg_id == msg.msg_id) && (this.portnum == msg.portnum);
//        }
//        return false;
//    }


}
