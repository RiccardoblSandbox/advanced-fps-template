package com.mygame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.ArmatureMask;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.BlendAction;
import com.jme3.anim.tween.action.LinearBlendSpace;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.animation.Track;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.phonon.scene.emitters.PositionalSoundEmitterControl;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

public class Jesse extends Node{

    private Node jesse;

    float fpsAngles[]=new float[3];

    public Node getJesse() {
        return jesse;
    }


    public Jesse(AssetManager assetManager,boolean fpsMode){

        jesse=(Node)assetManager.loadModel("fpstemplate/Jesse.gltf");

        PositionalSoundEmitterControl footsteps=new PositionalSoundEmitterControl(assetManager,"fpstemplate/metalwalk1.f32le");
        footsteps.setVolume(4f);
        addControl(footsteps);
        
        addControl(new AbstractControl(){
            float t=0;
            @Override
            protected void controlUpdate(float tpf) {
                boolean walking=false;
                BetterCharacterControl bc=getControl(BetterCharacterControl.class);
                
                if(bc!=null){
                    walking=(bc.isOnGround()&&bc.getRigidBody().getLinearVelocity().length()>0.1);
                }

                if(walking){
                    t+=tpf;
                    if(t>0.3){
                        t=0;
                        footsteps.playInstance();
                    }
                }
            }

            @Override
            protected void controlRender(RenderManager rm, ViewPort vp) {}            
        });

        if(!fpsMode)attachChild(jesse);


        Collection<AnimComposer> animComposers=new ArrayList<AnimComposer>();
        jesse.depthFirstTraversal(sx -> {
            AnimComposer cc=sx.getControl(AnimComposer.class);
            if(cc != null){
                animComposers.add(cc);
            }
        });
        

        if(fpsMode){
            jesse.getChild("body").setCullHint(CullHint.Always);

            BetterCharacterControl characterControl = new BetterCharacterControl(0.5f, 2, 50f);
            characterControl.setJumpForce(new Vector3f(0, 600, 0));
            addControl(characterControl);

        }

        animComposers.forEach(c->{
                SkinningControl skin=c.getSpatial().getControl(SkinningControl.class);
                skin.setHardwareSkinningPreferred(true);

                Armature armature=skin.getArmature();
                
                ArmatureMask mask=ArmatureMask.createMask(armature, "thigh.L");
                mask.addFromJoint(armature,"thigh.R");
                c.makeLayer("Legs",  mask);

                c.makeLayer("RightArm",  ArmatureMask.createMask(armature, "shoulder.R"));
                c.makeLayer("LeftArm",  ArmatureMask.createMask(armature, "shoulder.L"));
                c.makeLayer("Body",  ArmatureMask.createMask(armature, "spine","chest"));


                Supplier<Float> runBlend=()->{
                    for(int i=0;i<getNumControls();i++){
                        Control cc=getControl(i);
                        if(cc instanceof BetterCharacterControl){
                            BetterCharacterControl rb=(BetterCharacterControl)cc;
                            float v= FastMath.clamp(rb.getRigidBody().getLinearVelocity().length(),0f,1f);;
                            return v;
                        }
                    }
                    return 0f;
                };


                Supplier<Float> jumpBlend=()->{
                    for(int i=0;i<getNumControls();i++){
                        Control cc=getControl(i);
                        if(cc instanceof BetterCharacterControl){
                            BetterCharacterControl rb=(BetterCharacterControl)cc;
                            return rb.isOnGround()?0f:1f;
                        }
                    }
                    return 0f;
                };

               c.actionBlended("StandRun", new FunctionalBlendSpace(runBlend), "Stand","Run");
            //    c.actionBlended("StandRunJump", new FunctionalBlendSpace(jumpBlend), "StandRun","Jump");
               c.actionBlended("AimShake", new FunctionalBlendSpace(runBlend), "aim","shake");

                c.setCurrentAction("StandRun", "Legs");
                c.setCurrentAction("AimShake", "RightArm");

        });

   


        // playAnim(1,JesseAnimations.Run,true);
        // playAnim( 2,JesseAnimations.Crouch, true);
        // playAnim( 0,JesseAnimations.shake, true);
        // Spatial scam=jesse.getChild("camera");
        // fpsCam.setLocation(scam.getWorldTranslation());
        // fpsCam.setRotation(scam.getWorldRotation());         
    }

    public void updateFPSCamera(Camera sceneCamera, Camera fpsCam) {
        // this.depthFirstTraversal(sx -> {
        //     System.out.println(sx);
        // });
        Spatial scam=jesse.getChild("camera");
        Spatial lookAtS=jesse.getChild("camera_Orientation");

        jesse.setCullHint(CullHint.Never);

        fpsCam.setLocation(scam.getWorldTranslation().add(0,0,0));
        fpsCam.lookAt(lookAtS.getWorldTranslation(),Vector3f.UNIT_Y);
        // fpsCam.setLocation(fpsCam.getLocation().add(0,0, 0));

        float sceneAngles[]=new float[3];
        sceneCamera.getRotation().toAngles(sceneAngles);

        fpsCam.getRotation().toAngles(fpsAngles);

        fpsAngles[0]=sceneAngles[0];

        // lookDownUpActions.forEach(a->{            
        //     float v=1.f-(FastMath.clamp(fpsAngles[0]/1.14f,-1f,1f)*0.5f+0.5f);
        //     a.getBlendSpace().setValue(v);
        // });

        fpsCam.setRotation(fpsCam.getRotation().fromAngles(fpsAngles));

        float fov=80.6f;
        float radfov=fov * FastMath.PI / 180.0F;
        float fovy=(float)(2.0F * FastMath.atan(FastMath.tan(radfov / 2.0F) * (float)fpsCam.getHeight() / (float)fpsCam.getWidth()));
        fovy=FastMath.ceil(fovy * 180.0F / FastMath.PI);
        fpsCam.setFrustumPerspective(fovy,(float)fpsCam.getWidth() / (float)fpsCam.getHeight(),0.01f,100f);

        // fpsCam.setRotation(scam.getWorldRotation());         

    }
    
    
    // public void forEachAnimComposer(Consumer<AnimComposer> c) {
    
    // }

    // public Collection<AnimLayer> getLayer(int layer, Collection<AnimLayer> out) {

    //     if(out == null){
    //         out=new ArrayList<AnimLayer>();
    //     }
    //     for(AnimMatch m:animMatches){
    //         while(m.animLayers.size() <= layer){
    //             String layerN="Layer" + m.animLayers.size();
    //             AnimLayer l=new AnimLayer();
    //             l.layerName=layerN;
    //             l.animComposer=m.animComposer;
    //             m.animLayers.add(l);
    //             m.animComposer.makeLayer(l.layerName,t -> true);
    //         }
    //         // m.animComposer.makeLayer(name, mask);
    //         out.add(m.animLayers.get(layer));
    //     }
    //     return out;
    // }

    // ArrayList<AnimLayer> tmpCh=new ArrayList<AnimLayer>();

    // public void playAnim(int layer, JesseAnimations anim, boolean loop) {
    //     tmpCh.clear();
    //     getLayer(layer,tmpCh);
    //     for(AnimLayer animC:tmpCh){
    //         System.out.println("Play anim " + anim + " in  " + animC);
    //         // animC.animComposer.setCurrentAction(anim.name(),animC.layerName);

    //         // BlendAction blended=animC.animComposer.actionBlended("run&shake",new LinearBlendSpace(0f,1f),"Run","shake");
    //         // blended.getBlendSpace().setValue(.5f);
    //         animC.animComposer.setCurrentAction("Stand");

    //         animC.animComposer.makeLayer("Left", MaskOnlyInfluencedBones.newMask(animC.animComposer.getAnimClip("touch")));
    //         Action a=animC.animComposer.action("touch");

    //     }
    // }
}