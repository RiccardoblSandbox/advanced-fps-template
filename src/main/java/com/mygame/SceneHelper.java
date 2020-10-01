package com.mygame;

import com.jayfella.filter.BokehDoF.BokehDoFFilter;
import com.jayfella.filter.LensFlare.LensFlareFilter;
import com.jayfella.filter.MipMapBloom.MipmapBloomFilter;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowFilter;

import static com.mygame.Main.NumSamples;

public class SceneHelper {

    public enum Effect {
        Ambient_Occlusion,
        Bloom,
        MipMapBloom,
        Directional_Shadows,
        FXAA,
        BokehDof,
        ToneMapping,
        LensFlare,
    }

    private final AssetManager assetManager;
    private final ViewPort viewPort;
    private final DirectionalLight directionalLight;
    private final FilterPostProcessor fpp;

    public SceneHelper(AssetManager assetManager, ViewPort viewPort, DirectionalLight directionalLight) {
        this.assetManager = assetManager;
        this.viewPort = viewPort;
        this.directionalLight = directionalLight;

        fpp = new FilterPostProcessor(assetManager);
        fpp.setNumSamples(NumSamples);

        this.viewPort.addProcessor(fpp);
    }

    /**
     * Add a post-processing effect to the scene.
     * @param effects The effect to enable.
     */
    public void addEffect(Effect... effects) {

        for (Effect effect : effects) {

            switch (effect) {

                case Directional_Shadows: {

                    if (fpp.getFilter(DirectionalLightShadowFilter.class) != null) {
                        break;
                    }

                    DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 2048, 3);
                    dlsf.setLight(directionalLight);
                    fpp.addFilter(dlsf);

                    break;
                }

                case Ambient_Occlusion: {

                    if (fpp.getFilter(SSAOFilter.class) != null) {
                        break;
                    }

                    SSAOFilter ssaoFilter = new SSAOFilter();
                    fpp.addFilter(ssaoFilter);

                    break;
                }

                case Bloom: {

                    if (fpp.getFilter(BloomFilter.class) != null) {
                        break;
                    }

                    BloomFilter bloomFilter = new BloomFilter();
                    bloomFilter.setExposurePower(55);
                    bloomFilter.setBloomIntensity(1.0f);
                    fpp.addFilter(bloomFilter);

                    break;
                }

                case MipMapBloom: {

                    if (fpp.getFilter(MipmapBloomFilter.class) != null) {
                        break;
                    }

                    MipmapBloomFilter bloomFilter = new MipmapBloomFilter();
                    fpp.addFilter(bloomFilter);

                    break;
                }

                case FXAA: {

                    if (fpp.getFilter(FXAAFilter.class) != null) {
                        break;
                    }

                    FXAAFilter fxaaFilter = new FXAAFilter();
                    fpp.addFilter(fxaaFilter);

                    break;
                }

                case ToneMapping: {

                    if (fpp.getFilter(ToneMapFilter.class) != null) {
                        break;
                    }

                    ToneMapFilter toneMapFilter = new ToneMapFilter();
                    fpp.addFilter(toneMapFilter);

                    break;
                }

                case BokehDof: {

                    if (fpp.getFilter(BokehDoFFilter.class) != null) {
                        break;
                    }

                    BokehDoFFilter doFFilter = new BokehDoFFilter();
                    doFFilter.setFocusPoint(16);
                    fpp.addFilter(doFFilter);

                    break;
                }

                case LensFlare: {

                    if (fpp.getFilter(LensFlareFilter.class) != null) {
                        break;
                    }

                    LensFlareFilter lensFlareFilter = new LensFlareFilter();
                    fpp.addFilter(lensFlareFilter);
                }

            }

        }

    }

    public LightProbe loadDefaultLightProbe() {
        Node probeNode = (Node) assetManager.loadModel("Probes/lightprobe.j3o");
        LightProbe lightProbe = (LightProbe) probeNode.getLocalLightList().get(0);

        lightProbe.getArea().setRadius(100);

        return lightProbe;
    }

}
