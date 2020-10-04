package com.mygame;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class BasicCharacterMovementState extends BaseAppState implements ActionListener, AnalogListener {

    private final BetterCharacterControl characterControl;

    private float rotationSpeed = 1f;
    private boolean invertY = false;
    private boolean left = false, right = false, up = false, down = false, run = false;

    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private Vector3f walkDirection = new Vector3f();

    private Camera cam;

    public BasicCharacterMovementState(BetterCharacterControl characterControl) {
        this.characterControl = characterControl;
    }

    @Override
    protected void initialize(Application app) {
        this.cam = app.getCamera();
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        mapInput();
    }

    @Override
    protected void onDisable() {
        unmapInput();
    }

    private void mapInput() {

        InputManager inputManager = getApplication().getInputManager();

        inputManager.setCursorVisible(false);

        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));

        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Run", new KeyTrigger(KeyInput.KEY_LSHIFT));

        inputManager.addMapping("Rotate_Left", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("Rotate_Right", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("Rotate_Up", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("Rotate_Down", new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");

        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Run");

        inputManager.addListener(this, "Rotate_Left");
        inputManager.addListener(this, "Rotate_Right");
        inputManager.addListener(this, "Rotate_Up");
        inputManager.addListener(this, "Rotate_Down");
    }

    private void unmapInput() {
        InputManager inputManager = getApplication().getInputManager();

        inputManager.deleteMapping("Left");
        inputManager.deleteMapping("Right");
        inputManager.deleteMapping("Up");
        inputManager.deleteMapping("Down");

        inputManager.deleteMapping("Jump");
        inputManager.deleteMapping("Run");

        inputManager.deleteMapping("Rotate_Left");
        inputManager.deleteMapping("Rotate_Right");
        inputManager.deleteMapping("Rotate_Up");
        inputManager.deleteMapping("Rotate_Down");

        inputManager.removeListener(this);
    }

    float forwardSpeed = 2.6f;
    float sideSpeed = 2.4f;
    float walkSpeed = 5.0f;
    float runSpeed = 8.0f;

    @Override
    public void update(float tpf) {

        float speed = run ? runSpeed : walkSpeed;

        camDir.set(cam.getDirection()).multLocal(forwardSpeed).multLocal(speed);
        camLeft.set(cam.getLeft()).multLocal(sideSpeed).multLocal(speed);

        camDir.setY(0);
        camLeft.setY(0);

        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }

        if(characterControl.isOnGround())characterControl.setWalkDirection(walkDirection);
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {

        switch (binding) {

            case "Left": {
                left = isPressed;
                break;
            }

            case "Right": {
                right = isPressed;
                break;
            }

            case "Up": {
                up = isPressed;
                break;
            }

            case "Down": {
                down = isPressed;
                break;
            }

            case "Jump": {
                if (isPressed) {
                    characterControl.jump();
                }
                break;
            }

            case "Run": {
                run = isPressed;
            }

        }

    }


    @Override
    public void onAnalog(String name, float value, float tpf) {

        Vector3f initialUpVec = new Vector3f(0, 1, 0);

        switch (name) {

            case "Rotate_Left": {
                rotateCamera(value, initialUpVec);
                break;
            }

            case "Rotate_Right": {
                rotateCamera(-value, initialUpVec);
                break;
            }

            case "Rotate_Up": {
                rotateCamera(-value * (invertY ? -1 : 1), cam.getLeft());
                break;
            }

            case "Rotate_Down": {
                rotateCamera(value * (invertY ? -1 : 1), cam.getLeft());
                break;
            }

        }

    }


    /**
     * Rotate the camera by the specified amount around the specified axis.
     *
     * @param value rotation amount
     * @param axis direction of rotation (a unit vector)
     */
    protected void rotateCamera(float value, Vector3f axis){

        Matrix3f mat = new Matrix3f();
        mat.fromAngleNormalAxis(rotationSpeed * value, axis);

        Vector3f up = cam.getUp();
        Vector3f left = cam.getLeft();
        Vector3f dir = cam.getDirection();

        mat.mult(up, up);
        mat.mult(left, left);
        mat.mult(dir, dir);

        Quaternion q = new Quaternion();
        q.fromAxes(left, up, dir);
        q.normalizeLocal();

        cam.setAxes(q);
    }


}