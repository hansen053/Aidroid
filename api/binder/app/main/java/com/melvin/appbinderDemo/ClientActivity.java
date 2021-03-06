//实现过程

public void clickHandler(View view){
    switch (view.getId()){
        case R.id.btn_bind:
            bindRemoteService():
            break;
        case R.id.btn_unbind:
            unbindRemoteService();
            break;
        case R.id.btn_kill:
            killRemoteService();
            break;
    }
}


private ServiceConnection mConnection=new ServiceConnection(){
    @Override
    public void onServiceConnected(ComponetName name,IBinder service){
        mRemoteService=IRemoteService.Stub.asInterface(service);
        String pidInfo=null;
        try{
            MyData myData=mRemoteService.getMyData();
            pidInfo="pid="+mRemoteService.getPid()+
                ",data1="+mRemoteService.getData1()+
                ",data2="+mRemoteService.getData2();
        }catch(RemoteException e){
        }
    }

    @Override
    public void onServiceDisconnected(ComponetName name){
        mRemoteService=null;
    }
}
private void bindRemoteService(){
    Intent intent=new Intent(ClinetActivity.this,RemoteService.class);
    intent.setAction(IRemteService.class.getName());
    bindService(intent,mConnection,Context.BIND_AUTO_CREATE);
}

private void unbindRemoteService(){
    unbindService(mConnetion); 
}

private void killRemoteService(){
    android.os.Process.killProcess(mRemoteService.getPid());
}












