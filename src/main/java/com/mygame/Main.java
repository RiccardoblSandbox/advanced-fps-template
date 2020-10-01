package com.mygame;

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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

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

    @Override
    public void simpleInitApp() {

        // Configure the scene for PBR
        getRenderManager().setPreferredLightMode(TechniqueDef.LightMode.SinglePassAndImageBased);
        getRenderManager().setSinglePassLightBatchSize(10);

        // Enable physics...
        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(false); // enable to visualize physics meshes
        stateManager.attach(bulletAppState);

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
        sceneHelper.addEffect(
                SceneHelper.Effect.Directional_Shadows,
                SceneHelper.Effect.Ambient_Occlusion,
                // SceneHelper.Effect.Bloom,
                // SceneHelper.Effect.MipMapBloom,
                SceneHelper.Effect.ToneMapping,
                SceneHelper.Effect.BokehDof
                // SceneHelper.Effect.LensFlare
                // SceneHelper.Effect.FXAA
        );

        // Set our entire scene to cast and receive shadows.
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // load in a scene
        PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();

        Node scene = loadScene(physicsSpace);
        rootNode.attachChild(scene);

        // create our player
        playerNode = createCharacter(physicsSpace);
        rootNode.attachChild(playerNode);

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

        rootNode.attachChild(level);return level;
    }

    private Spatial pistol;
    private Node pistolNode;
    private Material bulletMaterial;

    private Node createCharacter(PhysicsSpace physicsSpace) {

        BetterCharacterControl characterControl = new BetterCharacterControl(0.5f, 2, 1);
        characterControl.setJumpForce(new Vector3f(0, 20, 0));

        Node playerNode = new Node("Player");
        playerNode.addControl(characterControl);

        physicsSpace.add(characterControl);

        characterControl.warp(new Vector3f(0, 5, 0));

        pistol = assetManager.loadModel("Models/Pistol.j3o");

        pistolNode = new Node("Pistol Node");
        pistolNode.attachChild(pistol);
        pistol.setLocalTranslation(-.2f, 1.9f, .3f);
        playerNode.attachChild(pistolNode);

        // define a material for our bullet that we'll re-use instead of creating a new one each time.
        bulletMaterial = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        bulletMaterial.setColor("BaseColor", ColorRGBA.Yellow.mult(0.5f));
        bulletMaterial.setFloat("Metallic", 0.01f);
        bulletMaterial.setFloat("Roughness", 0.3f);

        return playerNode;
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

            Geometry bullet = new Geometry("Bullet", new Sphere(32, 32, 0.1f));
            bullet.setMaterial(bulletMaterial);
            rootNode.attachChild(bullet);

            sceneBullets.add(new TimedBullet(bullet, 10));

            RigidBodyControl rigidBodyControl = new RigidBodyControl(CollisionShapeFactory.createDynamicMeshShape(bullet), 0.5f);
            rigidBodyControl.setCcdMotionThreshold(.2f);
            rigidBodyControl.setCcdSweptSphereRadius(.2f);
            bullet.addControl(rigidBodyControl);

            stateManager.getState(BulletAppState.class).getPhysicsSpace().add(rigidBodyControl);

            Vector3f bulletLocation = pistol.localToWorld(new Vector3f(0, 0.031449f, 0.2f), null);

            rigidBodyControl.setPhysicsLocation(bulletLocation);
            rigidBodyControl.setPhysicsRotation(cam.getRotation());
            rigidBodyControl.applyImpulse(cam.getDirection().mult(20), new Vector3f());
        }
    }

    private float[] angles = new float[3];
    Quaternion pistolRot = new Quaternion();

    @Override
    public void simpleUpdate(float tpf) {

        cam.setLocation(playerNode.getLocalTranslation().add(0, 2,0));

        playerNode.getControl(BetterCharacterControl.class).setViewDirection(cam.getDirection());

        cam.getRotation().toAngles(angles);
        pistolRot.fromAngles(angles[0], 0, 0);
        pistol.setLocalRotation(pistolRot);

        sceneBullets.removeIf(bullet -> {

            if (bullet.updateTime(tpf) > bullet.getMaxTime()) {
                bullet.getSpatial().removeFromParent();
                RigidBodyControl rigidBodyControl = bullet.getSpatial().getControl(RigidBodyControl.class);
                getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(rigidBodyControl);
                return true;
            }

            return false;
        });


    }

    private final List<TimedBullet> sceneBullets = new ArrayList<>();

    private static class TimedBullet {

        private final Spatial bullet;
        private final float maxTime;
        private float time;

        public TimedBullet(Spatial bullet, float maxTime) {
            this.bullet = bullet;
            this.maxTime = maxTime;
        }

        public Spatial getSpatial() {
            return bullet;
        }

        public float getMaxTime() {
            return maxTime;
        }

        public float getTime() {
            return time;
        }

        public float updateTime(float tpf) {
            time += tpf;
            return time;
        }

    }

}