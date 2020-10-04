package com.mygame;

import java.util.function.Supplier;

import com.jme3.anim.tween.action.BlendAction;
import com.jme3.anim.tween.action.BlendSpace;

public class FunctionalBlendSpace implements BlendSpace{
    Supplier<Float> weightFun;
    public FunctionalBlendSpace(Supplier<Float> weightFun){
        this.weightFun=weightFun;
    }

    @Override
    public void setBlendAction(BlendAction action) {

    }

    @Override
    public float getWeight() {
        return weightFun.get();
    }

    @Override
    public void setValue(float value) {

    }
    
}