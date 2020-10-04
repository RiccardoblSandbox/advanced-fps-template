package com.mygame;

import com.jme.effekseer.EffekseerRenderer;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.phonon.Phonon;
import com.jme3.phonon.PhononRenderer;
import com.jme3.phonon.desktop_javasound.JavaSoundPhononSettings;
import com.jme3.phonon.scene.emitters.SoundEmitterControl;
import com.jme3.phonon.scene.material.PhononMaterialPresets;
import com.jme3.phonon.scene.material.SingleMaterialGenerator;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.mygame.Jesse.Mode;

import wf.frk.f3b.jme3.F3bLoader;

import java.util.ArrayList;
import java.util.List;

public class Main extends SimpleApplication implements ActionListener {

    private Node playerNode;

    public static final int NumSamples = 1;

    public static void main(String[] args) {

        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("My Awesome Game");
        settings.setResolution(1280, 720);
        settings.setSamples(NumSamples);
        settings.setGammaCorrection(true);

        app.setShowSettings(true);
        app.setSettings(settings);
        app.start();

    }

    public Main() {
        // these are the default AppsStates minus the FlyCam
        super(new StatsAppState(), new AudioListenerState(), new DebugKeysAppState());
    }

    Node fpsRoot;
    Jesse jesse;
    Camera fpsCam;
    JavaSoundPhononSettings soundSettings;
    PhononRenderer soundEngine;
    @Override
    public void simpleInitApp() {
        try{
            soundSettings=new JavaSoundPhononSettings();
            soundSettings.materialGenerator=new SingleMaterialGenerator(PhononMaterialPresets.metal);
            soundEngine=Phonon.init(soundSettings, this );


        }catch(Exception e){
            throw new RuntimeException("Can't load audio engine",e);
        }


      

        EffekseerRenderer effekseerRenderer=EffekseerRenderer.addToViewPort(viewPort, assetManager,settings.isGammaCorrection());
        effekseerRenderer.setAsync(1);

        fpsCam=cam.clone();
        ViewPort fpsView=renderManager.createMainView("FPSView", fpsCam);

        fpsRoot=new Node("FPSRoot");
        fpsView.getScenes().add(fpsRoot);

        jesse=new Jesse(assetManager, Mode.ThirdPerson);
        fpsRoot.attachChild(jesse.getFPSNode());

        // Configure the scene for PBR
        getRenderManager().setPreferredLightMode(TechniqueDef.LightMode.SinglePassAndImageBased);
        getRenderManager().setSinglePassLightBatchSize(10);

        // Enable physics...
        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true); // enable to visualize physics meshes
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0,-18f,0));

        // Adjust to near frustum to a very close amount.
        float aspect = (float)cam.getWidth() / (float)cam.getHeight();
        cam.setFrustumPerspective(55, aspect, 0.01f, 1000);

        // change the viewport background color.
        viewPort.setBackgroundColor(new ColorRGBA(0.4f, 0.5f, 0.6f, 1.0f));

        // Add some lights
        DirectionalLight directionalLight = new DirectionalLight(
                new Vector3f(-1, -1, -1).normalizeLocal(),
                new ColorRGBA(1,1,1,1)
        );

        rootNode.addLight(directionalLight);

        // Create an instance of the SceneHelper class.
        SceneHelper sceneHelper = new SceneHelper(assetManager, viewPort, directionalLight);

        // load a pre-generated lightprobe.
        LightProbe lightProbe = sceneHelper.loadDefaultLightProbe();
        rootNode.addLight(lightProbe);

        // Add some effects
        // sceneHelper.addEffect(
        //         SceneHelper.Effect.Directional_Shadows,
        //         SceneHelper.Effect.Ambient_Occlusion,
        //         // SceneHelper.Effect.Bloom,
        //         // SceneHelper.Effect.MipMapBloom,
        //         SceneHelper.Effect.ToneMapping,
        //         SceneHelper.Effect.BokehDof
        //         // SceneHelper.Effect.LensFlare
        //         // SceneHelper.Effect.FXAA
        // );

        // Set our entire scene to cast and receive shadows.
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // load in a scene
        PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();

        Node scene = loadScene(physicsSpace);
        rootNode.attachChild(scene);

        // create our player
        playerNode = jesse;
        rootNode.attachChild(playerNode);
        physicsSpace.addAll(playerNode);

        // configure our input
        setupInput(playerNode.getControl(BetterCharacterControl.class));
    }

    private Node loadScene(PhysicsSpace physicsSpace) {
        Node level = (Node) assetManager.loadModel("Scenes/fps.j3o");

        Spatial statics = level.getChild("Static");
        RigidBodyControl staticRigids = new RigidBodyControl(CollisionShapeFactory.createMeshShape(statics), 0);
        statics.addControl(staticRigids);
        physicsSpace.add(staticRigids);

        Node moveables = (Node) level.getChild("Moveables");

        for (Spatial child : moveables.getChildren()) {
            RigidBodyControl rigidBodyControl = new RigidBodyControl(CollisionShapeFactory.createDynamicMeshShape(child), 1.0f);
            child.addControl(rigidBodyControl);
            physicsSpace.add(rigidBodyControl);
        }

        // Phonon.loadScene(soundSettings, this , level,null );

        // Add Background sounds
        SoundEmitterControl background=new SoundEmitterControl(assetManager,"fpstemplate/Bogart VGM - Scifi Concentration (looped).f32leS");
        rootNode.addControl(background);
        background.setLooping(true);
        background.setVolume(0.1f);
        background.play();
        
        rootNode.attachChild(level);return level;
    }

 


    private void setupInput(BetterCharacterControl characterControl) {

        inputManager.setCursorVisible(false);

        // set up the basic movement functions for our character.
        BasicCharacterMovementState characterMovementState = new BasicCharacterMovementState(characterControl);
        stateManager.attach(characterMovementState);

        // add a mapping for our shoot function.
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "Shoot");
    }

    public void onAction(String binding, boolean isPressed, float tpf) {

        if (binding.equals("Shoot") && !isPressed) {
            Spatial b=jesse.shot( assetManager,cam.getDirection());
            stateManager.getState(BulletAppState.class).getPhysicsSpace().addAll(b);
            rootNode.attachChild(b);

        //     Geometry bullet = new Geometry("Bullet", new Sphere(32, 32, 0.1f));
        //     bullet.setMaterial(bulletMaterial);
        //     rootNode.attachChild(bullet);


        //     RigidBodyControl rigidBodyControl = new RigidBodyControl(CollisionShapeFactory.createDynamicMeshShape(bullet), 0.5f);
        //     rigidBodyControl.setCcdMotionThreshold(.2f);
        //     rigidBodyControl.setCcdSweptSphereRadius(.2f);
        //     bullet.addControl(rigidBodyControl);

        //     stateManager.getState(BulletAppState.class).getPhysicsSpace().add(rigidBodyControl);

        //     Vector3f bulletLocation = pistol.localToWorld(new Vector3f(0, 0.031449f, 0.2f), null);

        //     rigidBodyControl.setPhysicsLocation(bulletLocation);
        //     rigidBodyControl.setPhysicsRotation(cam.getRotation());
        //     rigidBodyControl.applyImpulse(cam.getDirection().mult(20), new Vector3f());
        }
    }

    private float[] angles = new float[3];
    Quaternion pistolRot = new Quaternion();

    @Override
    public void simpleUpdate(float tpf) {
        fpsRoot.updateLogicalState(tpf);
        fpsRoot.updateGeometricState();
        jesse.updateCamera(cam,fpsCam);

        playerNode.getControl(BetterCharacterControl.class).setViewDirection(cam.getDirection());

        cam.getRotation().toAngles(angles);
        pistolRot.fromAngles(angles[0], 0, 0);
        // pistol.setLocalRotation(pistolRot);

    }

}