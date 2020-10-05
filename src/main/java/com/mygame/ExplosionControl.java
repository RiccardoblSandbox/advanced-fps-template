package com.mygame;

import java.util.function.Function;
import java.util.function.Supplier;

import com.jme.effekseer.EffekseerEmitterControl;
import com.jme.effekseer.driver.EffekseerEmissionDriverGeneric;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.cinematic.Cinematic;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.phonon.scene.emitters.PositionalSoundEmitterControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class ExplosionControl extends AbstractControl implements PhysicsCollisionListener{
    AssetManager assetManager;
    boolean listenerAttached=false;
    Float ignited=null;
    Function<PhysicsCollisionObject,Boolean>  filter;
    public ExplosionControl(AssetManager assetManager,Function<PhysicsCollisionObject,Boolean> filter){
        this.assetManager=assetManager;
        this.filter=filter;
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        if(ignited!=null)return;
        RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);

        if(
            !((event.getObjectA()==rb&&filter.apply(event.getObjectB()))|| (event.getObjectB()==rb&&filter.apply(event.getObjectA())))
        ){
            return;
        }

        
   
        ignited=2f;
        System.out.println("Collision!");
        EffekseerEmitterControl flame=new EffekseerEmitterControl(assetManager,"fpstemplate/effekts/pierre01/flame.efkefc");
        spatial.addControl(flame);
        rb.setEnabled(false);
        EffekseerEmissionDriverGeneric driver=(EffekseerEmissionDriverGeneric)flame.getDriver();
        // driver.onDestruction(i -> {
        //     rb.getPhysicsSpace().removeAll(spatial);
        //     spatial.removeFromParent();
        //     rb.getPhysicsSpace().removeCollisionListener(this);
        //     System.out.println("Collision over");
        // });

        PositionalSoundEmitterControl expl=new PositionalSoundEmitterControl(assetManager,"fpstemplate/Explosion.f32le");
        spatial.addControl(expl);
        expl.setVolume(122.5f);
        expl.setCustomDirectSoundPathFunction((path)->{
            path.distanceAttenuation*=2;
            if(path.distanceAttenuation>1)path.distanceAttenuation=1;
        });
        expl.play();
        
      
    }


    @Override
    protected void controlUpdate(float tpf) {
        if(!listenerAttached){
            RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);
            if(rb==null){
                System.out.println("No rb:(");
                return;
            }
            rb.getPhysicsSpace().addCollisionListener(this);  
            listenerAttached=true;
        }
        if(ignited!=null&&ignited>0){
            ignited-=tpf;
            if(ignited<=0){
                RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);

                for(PhysicsRigidBody b:rb.getPhysicsSpace().getRigidBodyList()){
                    if(b.getMass()==0)continue;
                    Vector3f expCenter2Body=b.getPhysicsLocation().subtract(rb.getPhysicsLocation());
                    float distance=expCenter2Body.length();
                    float explosionRadius=10f;
                    float baseStrength=100f;
                    if(distance<explosionRadius){
                        float strength=(1.f-FastMath.clamp(distance/explosionRadius,0,1))*baseStrength;
                        b.setLinearVelocity(expCenter2Body.normalize().mult(strength));
                    }
                }
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }
    
}