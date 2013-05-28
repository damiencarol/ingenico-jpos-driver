package com.blitz.ingenico.thread;

public enum IngenicoSerialThreadState
{
    IDLE,

    RC_RECEIVED_ENQ, 
    RC_WAIT_STX,

    EM_SEND_ENQ, 
    EM_WAIT_ACK_AFTER_ENQ, 
    EM_SEND_DATA, 
    EM_WAIT_ACK_AFTER_DATA,
    EM_ERROR_DATA,

    RC_WAIT_LRC, 
    RC_WAIT_EOT, 
    RC_RECEIVING_DATA,

    PAUSE
}
