package com.mygame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.jme.effekseer.EffekseerEmitterControl;
import com.jme.effekseer.driver.EffekseerEmissionDriverGeneric;
import com.jme.effekseer.driver.fun.impl.EffekseerGenericSpawner;
import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.AnimationMask;
import com.jme3.anim.Armature;
import com.jme3.anim.ArmatureMask;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.BlendAction;
import com.jme3.anim.tween.action.BlendSpace;
import com.jme3.anim.tween.action.BlendableAction;
import com.jme3.anim.tween.action.LinearBlendSpace;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.animation.Track;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
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
    private Mode mode;
    float fpsAngles[]=new float[3];

    public Node getFPSNode() {
        return mode==Mode.FirstPerson?jesse:new Node();
    }

    public Spatial shot(AssetManager assetManager,Vector3f direction,Node parent,PhysicsSpace phy){
        Vector3f pos=getCameraPosition(false);
        // pos=pos.add(direction.mult(1));
        
        Node bullet=new Node("bullet");
        bullet.setLocalTranslation(pos);
        parent.attachChild(bullet);

        SphereCollisionShape shape=new SphereCollisionShape(0.1f);
        RigidBodyControl rb=new RigidBodyControl(shape,0.1f);
        bullet.addControl(rb);
        phy.add(rb);
        rb.setGravity(Vector3f.ZERO);

        // rb.setPhysicsLocation(pos);

        rb.setLinearVelocity(direction.mult(10f));
        plasmaSound.playInstance();


        EffekseerEmitterControl effekt=new EffekseerEmitterControl(assetManager,"fpstemplate/effekts/bullet/bullet.efkefc");
        bullet.addControl(effekt);
        bullet.addControl(new ExplosionControl(assetManager,(x)->x!=jesse.getControl(RigidBodyControl.class)));


        Spatial target=jesse.getChild("gunTip");
        EffekseerEmitterControl flash=new EffekseerEmitterControl(assetManager,"fpstemplate/effekts/flash/flash.efkefc");
        // flash.setDriver(new EffekseerEmissionDriverGeneric().onDestruction((i)->{
        //     target.removeControl(flash);
        // }));
        target.addControl(flash);

        return bullet;


    }

    PositionalSoundEmitterControl plasmaSound;

    public static enum Mode{
        FirstPerson,
        ThirdPerson
    }
    public Jesse(AssetManager assetManager,Mode mode){
        this.mode=mode;
        jesse=new Node("Jesse");

        Spatial jj=assetManager.loadModel("fpstemplate/Jesse.gltf");
        jj.setLocalScale(0.4f);

        BoundingBox bbox=(BoundingBox)jj.getWorldBound();
        jesse.attachChild(jj);
        jj.setLocalTranslation(0,bbox.getYExtent(),0);

      

        if(mode==Mode.FirstPerson||mode==Mode.ThirdPerson){
            BetterCharacterControl characterControl = new BetterCharacterControl(bbox.getXExtent(), bbox.getYExtent()*2, 50f);
            characterControl.setJumpForce(new Vector3f(0, 600, 0));
            addControl(characterControl);
        }

        if(mode==Mode.FirstPerson){            
            Spatial sp=jesse.clone();
            sp.setCullHint(CullHint.Always);
            attachChild(sp);
            jesse.getChild("body").setCullHint(CullHint.Always);       
        }else{
            attachChild(jesse);
        }

        PositionalSoundEmitterControl footsteps=new PositionalSoundEmitterControl(assetManager,"fpstemplate/walk.f32le");
        addControl(footsteps);
        footsteps.setVolume(0.5f);
        
        PositionalSoundEmitterControl jumpSound=new PositionalSoundEmitterControl(assetManager,"fpstemplate/jump.f32le");
        addControl(jumpSound);

         plasmaSound=new PositionalSoundEmitterControl(assetManager,"fpstemplate/plasma.f32le");
        addControl(plasmaSound);
       
        addControl(new AbstractControl(){
            float t=0;
            boolean wasOnGround=true;
            @Override
            protected void controlUpdate(float tpf) {
                boolean walking=false;
                BetterCharacterControl bc=getControl(BetterCharacterControl.class);
                
                if(bc!=null){
                    walking=(bc.isOnGround()&&bc.getRigidBody().getLinearVelocity().length()>0.1);
                    if(!bc.isOnGround()&&wasOnGround){
                        jumpSound.playInstance();
                    }
                    wasOnGround=bc.isOnGround();
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



        Collection<AnimComposer> animComposers=new ArrayList<AnimComposer>();
        jesse.depthFirstTraversal(sx -> {
            AnimComposer cc=sx.getControl(AnimComposer.class);
            if(cc != null){
                animComposers.add(cc);
            }
        });
        

        LinearBlendSpace standWalkRunBlendSpace = new LinearBlendSpace(1, 10);
        LinearBlendSpace standWalkRunJumpBlendSpace = new LinearBlendSpace(0, 1);
        LinearBlendSpace aimShakeBlendSpace = new LinearBlendSpace(0, 1);

        AnimComposer ac = jesse.getChild("Jesse").getControl(AnimComposer.class);
        addControl(new AbstractControl() {


            @Override
            protected void controlUpdate(float tpf) {
                BetterCharacterControl bc = getControl(BetterCharacterControl.class);

                if (bc != null) {
              

                    float v = FastMath.clamp(bc.getRigidBody().getLinearVelocity().length(), 0, 14);
                    aimShakeBlendSpace.setValue(v < 1 ? 0.01f : 1); // setting 0 looks buggy!

                    v = FastMath.unInterpolateLinear(v, 0, 14);
                    v = FastMath.interpolateLinear(v, 1, 10);

                    standWalkRunBlendSpace.setValue(v);
                    ac.getAction("StandWalkRun").setSpeed(v);

                    //walkRunBlendSpace.setValue(v);

                    if(bc.isOnGround()) {
                        standWalkRunJumpBlendSpace.setValue(0);
                        ac.getAction("StandWalkRunJump").setSpeed(v);
                    } else {
                        standWalkRunJumpBlendSpace.setValue(1);
                        ac.getAction("StandWalkRunJump").setSpeed(20);
                    }
                }


            }

            @Override
            protected void controlRender(RenderManager rm, ViewPort vp) {
            }
        });


        SkinningControl skin = ac.getSpatial().getControl(SkinningControl.class);
        skin.setHardwareSkinningPreferred(true);

        Armature armature = skin.getArmature();

        ArmatureMask legsMask = ArmatureMask.createMask(armature, "thigh.L");
        legsMask.addFromJoint(armature, "thigh.R");
        ac.makeLayer("Legs", legsMask);


        ac.makeLayer("RightArm", ArmatureMask.createMask(armature, "shoulder.R"));
        ac.makeLayer("LeftArm", ArmatureMask.createMask(armature, "shoulder.L"));
        ac.makeLayer("Body", ArmatureMask.createMask(armature, "spine", "chest"));


        blendActions(ac, "StandWalkRun", standWalkRunBlendSpace, "Stand", "Walk", "Run");
        blendActions(ac, "StandWalkRunJump", standWalkRunJumpBlendSpace, "StandWalkRun","JumpMid");
        blendActions(ac, "AimShake", aimShakeBlendSpace, "aim", "shake");

        ac.setCurrentAction("StandWalkRunJump");//"Legs"
        ac.setCurrentAction("AimShake", "RightArm");


        // playAnim(1,JesseAnimations.Run,true);
        // playAnim( 2,JesseAnimations.Crouch, true);
        // playAnim( 0,JesseAnimations.shake, true);
        // Spatial scam=jesse.getChild("camera");
        // fpsCam.setLocation(scam.getWorldTranslation());
        // fpsCam.setRotation(scam.getWorldRotation());
   


        // playAnim(1,JesseAnimations.Run,true);
        // playAnim( 2,JesseAnimations.Crouch, true);
        // playAnim( 0,JesseAnimations.shake, true);
        // Spatial scam=jesse.getChild("camera");
        // fpsCam.setLocation(scam.getWorldTranslation());
        // fpsCam.setRotation(scam.getWorldRotation());         
    }
    public void blendActions(AnimComposer ac, String name, BlendSpace blendSpace, String... clips) {
        BlendableAction[] acts = new BlendableAction[clips.length];
        for (int i = 0; i < acts.length; i++) {
            BlendableAction ba = (BlendableAction) ac.action(clips[i]);
            acts[i] = ba;
        }
        BlendAction action = new BlendAction(blendSpace, acts) {
            @Override
            public void setMask(AnimationMask mask) {
                super.setMask(mask);
                for (Action action : actions) {
                    action.setMask(mask);
                }
            }
        };

        ac.addAction(name, action);
    }


    public Vector3f getCameraPosition(boolean fps){
        Spatial scam=fps?jesse.getChild("camera"):getChild("camera");
        return scam.getWorldTranslation();
    }

    public Vector3f getCameraDirection(boolean fps){
        Vector3f pos=getCameraPosition(fps);
        Spatial lookAtS=fps?jesse.getChild("camera_Orientation"):getChild("camera_Orientation");
        return lookAtS.getWorldTranslation().subtract(pos);    
    }
    

    public void updateCamera(Camera sceneCamera, Camera fpsCam) {
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
        
        
        if(mode==Mode.FirstPerson){
            sceneCamera.setLocation(getCameraPosition(false));
        }else{
            sceneCamera.setLocation(getCameraPosition(false).add(sceneCamera.getDirection().mult(-10)));

        }

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