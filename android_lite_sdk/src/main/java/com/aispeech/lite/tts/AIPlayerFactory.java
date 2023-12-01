package com.aispeech.lite.tts;


import com.aispeech.lite.param.TTSParams;

/**
 * 说明： 播放器抽象工厂类
 * 
 * @author Everett Li
 * @date Nov 12, 2014
 * @version 1.0
 */
public  class AIPlayerFactory {
    /**
     * 创建播放器
     * 
     * @return
     */
    public IAIPlayer createAIPlayer(TTSParams params) {
    	IAIPlayer player = null;
    	if(TTSParams.TYPE_CLOUD.equals(params.getType())&& !params.isRealBack()) {
    		player =  new AIMediaPlayer();
    	} else if(params.isRealBack()) {
    		player = new AIAudioTrack();
    	}
        return player;
    }
    

    /**
     * 创建数据块阻塞队列
     * 
     * @return
     */
    public SynthesizedBlockQueue createSynthesizedBlockQueue() {
        return new SynthesizedBlockQueue();
    }

}
