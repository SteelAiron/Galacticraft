package micdoodle8.mods.galacticraft.planets.asteroids.client.render.entity;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.perlin.NoiseModule;
import micdoodle8.mods.galacticraft.core.perlin.generator.Gradient;
import micdoodle8.mods.galacticraft.planets.GalacticraftPlanets;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityAstroMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Random;

public class RenderAstroMiner extends Render<EntityAstroMiner>
{
	private static final float LSIZE = 0.12F;
	private static final float RETRACTIONSPEED = 0.02F;
    private float spin;
    private float lastPartTime;

	public static ResourceLocation scanTexture;
    private OBJModel.OBJBakedModel mainModel;
    private OBJModel.OBJBakedModel hoverPadMain;
    private OBJModel.OBJBakedModel hoverPadGlow;
    private OBJModel.OBJBakedModel mainModelInactive;
    private OBJModel.OBJBakedModel modellaser1;
    private OBJModel.OBJBakedModel modellaser3;
    private OBJModel.OBJBakedModel modellasergl;

    private final NoiseModule wobbleX;
    private final NoiseModule wobbleY;
    private final NoiseModule wobbleZ;
    private final NoiseModule wobbleXX;
    private final NoiseModule wobbleYY;
    private final NoiseModule wobbleZZ;

    private void updateModels()
    {
        if (mainModel == null)
        {
            try
            {
                OBJModel model = (OBJModel) ModelLoaderRegistry.getModel(new ResourceLocation(GalacticraftPlanets.ASSET_PREFIX, "astro_miner_full.obj"));
                model = (OBJModel) model.process(ImmutableMap.of("flip-v", "true"));

                Function<ResourceLocation, TextureAtlasSprite> spriteFunction = location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
                mainModel = (OBJModel.OBJBakedModel) model.bake(new OBJModel.OBJState(ImmutableList.of("Hull", "Lasers"), false), DefaultVertexFormats.ITEM, spriteFunction);
                hoverPadMain = (OBJModel.OBJBakedModel) model.bake(new OBJModel.OBJState(ImmutableList.of("HoverPad"), false), DefaultVertexFormats.ITEM, spriteFunction);
                hoverPadGlow = (OBJModel.OBJBakedModel) model.bake(new OBJModel.OBJState(ImmutableList.of("Glow"), false), DefaultVertexFormats.ITEM, spriteFunction);
                modellaser1 = (OBJModel.OBJBakedModel) model.bake(new OBJModel.OBJState(ImmutableList.of("Main_Laser_Front"), false), DefaultVertexFormats.ITEM, spriteFunction);
                modellaser3 = (OBJModel.OBJBakedModel) model.bake(new OBJModel.OBJState(ImmutableList.of("Main_Laser_Center"), false), DefaultVertexFormats.ITEM, spriteFunction);
                modellasergl = (OBJModel.OBJBakedModel) model.bake(new OBJModel.OBJState(ImmutableList.of("Main_Laser_Left_Guard"), false), DefaultVertexFormats.ITEM, spriteFunction);

                model = (OBJModel) ModelLoaderRegistry.getModel(new ResourceLocation(GalacticraftPlanets.ASSET_PREFIX, "astro_miner_full_off.obj"));
                model = (OBJModel) model.process(ImmutableMap.of("flip-v", "true"));

                mainModelInactive = (OBJModel.OBJBakedModel) model.bake(new OBJModel.OBJState(ImmutableList.of("Hull", "Lasers", "HoverPad"), false), DefaultVertexFormats.ITEM, spriteFunction);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    static
    {
	    scanTexture = new ResourceLocation(GalacticraftPlanets.ASSET_PREFIX, "textures/misc/gradient.png");
    }

    public RenderAstroMiner(RenderManager renderManager)
    {
        super(renderManager);
        this.shadowSize = 2F;

        Random rand = new Random();
        this.wobbleX = new Gradient(rand.nextLong(), 2, 1);
        this.wobbleX.amplitude = 0.5F;
        this.wobbleX.frequencyX = 0.025F;

        this.wobbleY = new Gradient(rand.nextLong(), 2, 1);
        this.wobbleY.amplitude = 0.6F;
        this.wobbleY.frequencyX = 0.025F;

        this.wobbleZ = new Gradient(rand.nextLong(), 2, 1);
        this.wobbleZ.amplitude = 0.1F;
        this.wobbleZ.frequencyX = 0.025F;

        this.wobbleXX = new Gradient(rand.nextLong(), 2, 1);
        this.wobbleXX.amplitude = 0.1F;
        this.wobbleXX.frequencyX = 0.8F;

        this.wobbleYY = new Gradient(rand.nextLong(), 2, 1);
        this.wobbleYY.amplitude = 0.15F;
        this.wobbleYY.frequencyX = 0.8F;

        this.wobbleZZ = new Gradient(rand.nextLong(), 2, 1);
        this.wobbleZZ.amplitude = 0.04F;
        this.wobbleZZ.frequencyX = 0.8F;
    }

    @Override
    public void doRender(EntityAstroMiner astroMiner, double x, double y, double z, float f, float partialTickTime)
    {
        int ais = astroMiner.AIstate;
        boolean active = ais > EntityAstroMiner.AISTATE_ATBASE;

        float time = astroMiner.ticksExisted + partialTickTime;
        float sinOfTheTime = (MathHelper.sin(time / 4) + 1F)/4F + 0.5F;
        float wx = active ? this.wobbleX.getNoise(time) + this.wobbleXX.getNoise(time) : 0F;
        float wy = active ? this.wobbleY.getNoise(time) + this.wobbleYY.getNoise(time) : 0F;
        float wz = active ? this.wobbleZ.getNoise(time) + this.wobbleZZ.getNoise(time) : 0F;

        float partTime = partialTickTime - lastPartTime;
        lastPartTime = partialTickTime;
        while (partTime < 0) partTime += 1F;

        updateModels();

        this.bindTexture(TextureMap.locationBlocksTexture);

        if (Minecraft.isAmbientOcclusionEnabled())
        {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        }
        else
        {
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }

//      RenderHelper.enableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPushMatrix();
        final float rotPitch = astroMiner.prevRotationPitch + (astroMiner.rotationPitch - astroMiner.prevRotationPitch) * partialTickTime;
        final float rotYaw = astroMiner.prevRotationYaw + (astroMiner.rotationYaw - astroMiner.prevRotationYaw) * partialTickTime;

        GL11.glTranslatef((float)x, (float)y + 1.4F, (float)z);
        float partBlock;
        switch (astroMiner.facing)
        {
        case DOWN:
        	partBlock = (float) (astroMiner.posY % 1D);
        	break;
        case UP:
        	partBlock = 1F - (float) (astroMiner.posY % 1D);
        	break;
        case NORTH:
        	partBlock = (float) (astroMiner.posZ % 1D);
        	break;
        case SOUTH:
        	partBlock = 1F - (float) (astroMiner.posZ % 1D);
        	break;
        case WEST:
        	partBlock = (float) (astroMiner.posX % 1D);
        	break;
        case EAST:
        	partBlock = 1F - (float) (astroMiner.posX % 1D);
        	break;
        default:
        	partBlock = 0F;
        }
        partBlock /= 0.06F;

//        else if (rotPitch > 0F)
//        {
//            GL11.glTranslatef(-0.65F, -0.65F, 0);
//        	GL11.glRotatef(rotPitch, 0, 0, 1);
//            GL11.glTranslatef(0.65F, 0.65F, 0);
//        }
        GL11.glRotatef(rotYaw + 180F, 0, 1, 0);
        if (rotPitch != 0F)
        {
            GL11.glTranslatef(-0.65F, -0.65F, 0);
        	GL11.glRotatef(rotPitch / 4F, 1, 0, 0);
            GL11.glTranslatef(0.65F, 0.65F, 0);
        }

        GL11.glTranslatef(0F, -0.42F, 0.28F);
        GL11.glScalef(0.0495F, 0.0495F, 0.0495F);
        GL11.glTranslatef(wx, wy, wz);

        if (active)
        {
            this.drawBakedModel(mainModel);

	        renderLaserModel(astroMiner.retraction);

	        float lightMapSaveX = OpenGlHelper.lastBrightnessX;
	        float lightMapSaveY = OpenGlHelper.lastBrightnessY;
	        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
	        GL11.glDisable(GL11.GL_LIGHTING);
	        GL11.glColor4f(sinOfTheTime, sinOfTheTime, sinOfTheTime, 1.0F);
            this.drawBakedModel(hoverPadMain);

	        GL11.glDisable(GL11.GL_CULL_FACE);
	        GL11.glDisable(GL11.GL_ALPHA_TEST);
	        GL11.glDepthMask(false);
	        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
	        GL11.glEnable(GL11.GL_BLEND);
	        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
	        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
	        GL11.glColor4f(sinOfTheTime, sinOfTheTime, sinOfTheTime, 0.6F);
            this.drawBakedModel(hoverPadGlow);

	        if (ais < EntityAstroMiner.AISTATE_DOCKING)
	        {
	        	FMLClientHandler.instance().getClient().renderEngine.bindTexture(scanTexture);
		        final Tessellator tess = Tessellator.getInstance();
		        GL11.glColor4f(0, 0.6F, 1.0F, 0.2F);
                WorldRenderer worldRenderer = tess.getWorldRenderer();
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		        worldRenderer.pos(15.6F, -0.6F, -20F).tex(0D, 0D).endVertex();
		        worldRenderer.pos(37.8F, 31.4F, -45F - partBlock).tex(1D, 0D).endVertex();
		        worldRenderer.pos(37.8F, -32.6F, -45F - partBlock).tex(1D, 1D).endVertex();
		        worldRenderer.pos(15.6F, -0.7F, -20F).tex(0D, 1D).endVertex();
		        tess.draw();
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		        worldRenderer.pos(-15.6F, -0.6F, -20F).tex(0D, 0D).endVertex();
		        worldRenderer.pos(-37.8F, 31.4F, -45F - partBlock).tex(1D, 0D).endVertex();
		        worldRenderer.pos(-37.8F, -32.6F, -45F - partBlock).tex(1D, 1D).endVertex();
		        worldRenderer.pos(-15.6F, -0.7F, -20F).tex(0D, 1D).endVertex();
		        tess.draw();

		        int removeCount = 0;
		        int afterglowCount = 0;
		        GL11.glPopMatrix();
		        GL11.glPushMatrix();
		        GL11.glTranslatef((float)(x - astroMiner.posX), (float)(y - astroMiner.posY), (float)(z - astroMiner.posZ));
		        for (Integer blockTime : new ArrayList<Integer>(astroMiner.laserTimes))
		        {
		        	if (blockTime < astroMiner.ticksExisted - 19) removeCount++;
		        	else if (blockTime < astroMiner.ticksExisted - 3) afterglowCount++;
		        }
		        if (removeCount > 0) astroMiner.removeLaserBlocks(removeCount);
		        int count = 0;
		        for (BlockVec3 blockLaser : new ArrayList<BlockVec3>(astroMiner.laserBlocks))
		        {
		        	if (count < afterglowCount)
		        	{
			        	int fade = astroMiner.ticksExisted - astroMiner.laserTimes.get(count) - 8;
			        	if (fade < 0) fade = 0;
		        		doAfterGlow(blockLaser, fade);
		        	}
		        	else doLaser(astroMiner, blockLaser);
		        	count ++;
		        }
		        if (astroMiner.retraction > 0F)
		        {
		        	astroMiner.retraction -= RETRACTIONSPEED * partTime;
		        	if (astroMiner.retraction < 0F) astroMiner.retraction = 0F;
		        }
		        GL11.glPopMatrix();
	        }
	        else
	        {
		        if (astroMiner.retraction < 1F)
		        {
		        	astroMiner.retraction += RETRACTIONSPEED * partTime;
		        	if (astroMiner.retraction > 1F) astroMiner.retraction = 1F;
		        }
		        GL11.glPopMatrix();
	        }
	        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
	        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	        GL11.glDisable(GL11.GL_BLEND);
	        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	        GL11.glEnable(GL11.GL_CULL_FACE);
	        GL11.glEnable(GL11.GL_ALPHA_TEST);
	        GL11.glEnable(GL11.GL_LIGHTING);
	        GL11.glDepthMask(true);
	        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightMapSaveX, lightMapSaveY);
        }
        else
        {
            this.bindEntityTexture(astroMiner);
            this.drawBakedModel(mainModelInactive);
	        renderLaserModel(astroMiner.retraction);
	        if (astroMiner.retraction < 1F)
	        {
	        	astroMiner.retraction += RETRACTIONSPEED * partTime;
	        	if (astroMiner.retraction > 1F) astroMiner.retraction = 1F;
	        }
            GL11.glPopMatrix();
        }
    }

	private void doAfterGlow(BlockVec3 blockLaser, int level)
    {
        GL11.glPushMatrix();
        GL11.glTranslatef(blockLaser.x, blockLaser.y, blockLaser.z);
        final Tessellator tess = Tessellator.getInstance();
        WorldRenderer worldRenderer = tess.getWorldRenderer();
        GL11.glColor4f(1.0F, 0.7F, 0.7F, 0.016667F * (12 - level));
        float cA = -0.01F;
        float cB = 1.01F;
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cB, cA).tex(0D, 1D).endVertex();
        worldRenderer.pos(cB, cB, cA).tex(1D, 1D).endVertex();
        worldRenderer.pos(cB, cB, cB).tex(1D, 0D).endVertex();
        worldRenderer.pos(cA, cB, cB).tex(0D, 0D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cA, cA).tex(0D, 0D).endVertex();
        worldRenderer.pos(cA, cA, cB).tex(0D, 1D).endVertex();
        worldRenderer.pos(cB, cA, cB).tex(1D, 1D).endVertex();
        worldRenderer.pos(cB, cA, cA).tex(1D, 0D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cA, cA).tex(1D, 0D).endVertex();
        worldRenderer.pos(cA, cB, cA).tex(0D, 0D).endVertex();
        worldRenderer.pos(cA, cB, cB).tex(0D, 1D).endVertex();
        worldRenderer.pos(cA, cA, cB).tex(1D, 1D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cB, cA, cA).tex(1D, 1D).endVertex();
        worldRenderer.pos(cB, cA, cB).tex(1D, 0D).endVertex();
        worldRenderer.pos(cB, cB, cB).tex(0D, 0D).endVertex();
        worldRenderer.pos(cB, cB, cA).tex(0D, 1D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cA, cA).tex(1D, 0D).endVertex();
        worldRenderer.pos(1F, cA, cA).tex(0D, 0D).endVertex();
        worldRenderer.pos(1F, 1F, cA).tex(0D, 1D).endVertex();
        worldRenderer.pos(cA, 1F, cA).tex(1D, 1D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(1F, cA, 1F).tex(1D, 1D).endVertex();
        worldRenderer.pos(cA, cA, 1F).tex(1D, 0D).endVertex();
        worldRenderer.pos(cA, 1F, 1F).tex(0D, 0D).endVertex();
        worldRenderer.pos(1F, 1F, 1F).tex(0D, 1D).endVertex();
        tess.draw();


        GL11.glPopMatrix();
	}

    private void doLaser(EntityAstroMiner entity, BlockVec3 blockLaser)
    {
        GL11.glPushMatrix();
        GL11.glTranslatef(blockLaser.x, blockLaser.y, blockLaser.z);
        final Tessellator tess = Tessellator.getInstance();
        WorldRenderer worldRenderer = tess.getWorldRenderer();
        GL11.glColor4f(1.0F, 0.7F, 0.7F, 0.2F);
        float cA = -0.01F;
        float cB = 1.01F;
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cB, cA).tex(0D, 1D).endVertex();
        worldRenderer.pos(cB, cB, cA).tex(1D, 1D).endVertex();
        worldRenderer.pos(cB, cB, cB).tex(1D, 0D).endVertex();
        worldRenderer.pos(cA, cB, cB).tex(0D, 0D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cA, cA).tex(0D, 0D).endVertex();
        worldRenderer.pos(cA, cA, cB).tex(0D, 1D).endVertex();
        worldRenderer.pos(cB, cA, cB).tex(1D, 1D).endVertex();
        worldRenderer.pos(cB, cA, cA).tex(1D, 0D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cA, cA).tex(1D, 0D).endVertex();
        worldRenderer.pos(cA, cB, cA).tex(0D, 0D).endVertex();
        worldRenderer.pos(cA, cB, cB).tex(0D, 1D).endVertex();
        worldRenderer.pos(cA, cA, cB).tex(1D, 1D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cB, cA, cA).tex(1D, 1D).endVertex();
        worldRenderer.pos(cB, cA, cB).tex(1D, 0D).endVertex();
        worldRenderer.pos(cB, cB, cB).tex(0D, 0D).endVertex();
        worldRenderer.pos(cB, cB, cA).tex(0D, 1D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(cA, cA, cA).tex(1D, 0D).endVertex();
        worldRenderer.pos(1F, cA, cA).tex(0D, 0D).endVertex();
        worldRenderer.pos(1F, 1F, cA).tex(0D, 1D).endVertex();
        worldRenderer.pos(cA, 1F, cA).tex(1D, 1D).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(1F, cA, 1F).tex(1D, 1D).endVertex();
        worldRenderer.pos(cA, cA, 1F).tex(1D, 0D).endVertex();
        worldRenderer.pos(cA, 1F, 1F).tex(0D, 0D).endVertex();
        worldRenderer.pos(1F, 1F, 1F).tex(0D, 1D).endVertex();
        tess.draw();

        GL11.glColor4f(1.0F, 0.79F, 0.79F, 0.17F);
        float bb = 1.7F;
        float cc = 0.4F;
        float radiansYaw = entity.rotationYaw * 0.017453292F;
        float radiansPitch = entity.rotationPitch * 0.017453292F / 4F;
        float mainLaserX = bb * MathHelper.sin(radiansYaw) * MathHelper.cos(radiansPitch);
        float mainLaserY = cc + bb * MathHelper.sin(radiansPitch);
        float mainLaserZ = bb * MathHelper.cos(radiansYaw) * MathHelper.cos(radiansPitch);

        mainLaserX += entity.posX - blockLaser.x;
        mainLaserY += entity.posY - blockLaser.y;
        mainLaserZ += entity.posZ - blockLaser.z;

        float xD = (mainLaserX - 0.5F);
        float yD = (mainLaserY - 0.5F);
        float zD = (mainLaserZ - 0.5F);
        float xDa = Math.abs(xD);
        float yDa = Math.abs(yD);
        float zDa = Math.abs(zD);

        float xx, yy, zz;

        if (entity.facing.getIndex() > EnumFacing.SOUTH.getIndex())
        {
            xx = ((xD < 0) ? cA : cB);
            drawLaserX(mainLaserX, mainLaserY, mainLaserZ, xx, 0.5F, 0.5F);
        }
        else if (entity.facing.getIndex() <= EnumFacing.UP.getIndex())
        {
            yy = ((yD < 0) ? cA : cB);
            drawLaserY(mainLaserX, mainLaserY, mainLaserZ, 0.5F, yy, 0.5F);
        }
        else
        {
            zz = ((zD < 0) ? cA : cB);
            drawLaserZ(mainLaserX, mainLaserY, mainLaserZ, 0.5F, 0.5F, zz);
        }

        GL11.glPopMatrix();
   	}

    private void drawLaserX(float x1, float y1, float z1, float x2, float y2, float z2)
    {
        final Tessellator tess = Tessellator.getInstance();
        WorldRenderer worldRenderer = tess.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1 - 0.01F, z1 - 0.01F).endVertex();
        worldRenderer.pos(x2, y2 - LSIZE, z2 - LSIZE).endVertex();
        worldRenderer.pos(x2, y2 + LSIZE, z2 - LSIZE).endVertex();
        worldRenderer.pos(x1, y1 + 0.01F, z1 - 0.01F).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1 - 0.01F, z1 + 0.01F).endVertex();
        worldRenderer.pos(x2, y2 - LSIZE, z2 + LSIZE).endVertex();
        worldRenderer.pos(x2, y2 + LSIZE, z2 + LSIZE).endVertex();
        worldRenderer.pos(x1, y1 + 0.01F, z1 + 0.01F).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1 - 0.01F, z1 - 0.01F).endVertex();
        worldRenderer.pos(x2, y2 - LSIZE, z2 - LSIZE).endVertex();
        worldRenderer.pos(x2, y2 - LSIZE, z2 + LSIZE).endVertex();
        worldRenderer.pos(x1, y1 - 0.01F, z1 + 0.01F).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1 + 0.01F, z1 + 0.01F).endVertex();
        worldRenderer.pos(x2, y2 + LSIZE, z2 + LSIZE).endVertex();
        worldRenderer.pos(x2, y2 + LSIZE, z2 - LSIZE).endVertex();
        worldRenderer.pos(x1, y1 + 0.01F, z1 - 0.01F).endVertex();
        tess.draw();
    }

    private void drawLaserY(float x1, float y1, float z1, float x2, float y2, float z2)
    {
        final Tessellator tess = Tessellator.getInstance();
        WorldRenderer worldRenderer = tess.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1 - 0.01F, y1, z1 - 0.01F).endVertex();
        worldRenderer.pos(x2 - LSIZE, y2, z2 - LSIZE).endVertex();
        worldRenderer.pos(x2 + LSIZE, y2, z2 - LSIZE).endVertex();
        worldRenderer.pos(x1 + 0.01F, y1, z1 - 0.01F).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1 - 0.01F, y1, z1 + 0.01F).endVertex();
        worldRenderer.pos(x2 - LSIZE, y2, z2 + LSIZE).endVertex();
        worldRenderer.pos(x2 + LSIZE, y2, z2 + LSIZE).endVertex();
        worldRenderer.pos(x1 + 0.01F, y1, z1 + 0.01F).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1 - 0.01F, y1, z1 - 0.01F).endVertex();
        worldRenderer.pos(x2 - LSIZE, y2, z2 - LSIZE).endVertex();
        worldRenderer.pos(x2 - LSIZE, y2, z2 + LSIZE).endVertex();
        worldRenderer.pos(x1 - 0.01F, y1, z1 + 0.01F).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1 + 0.01F, y1, z1 + 0.01F).endVertex();
        worldRenderer.pos(x2 + LSIZE, y2, z2 + LSIZE).endVertex();
        worldRenderer.pos(x2 + LSIZE, y2, z2 - LSIZE).endVertex();
        worldRenderer.pos(x1 + 0.01F, y1, z1 - 0.01F).endVertex();
        tess.draw();
    }

    private void drawLaserZ(float x1, float y1, float z1, float x2, float y2, float z2)
    {
        final Tessellator tess = Tessellator.getInstance();
        WorldRenderer worldRenderer = tess.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1 - 0.01F, y1 - 0.01F, z1).endVertex();
        worldRenderer.pos(x2 - LSIZE, y2 - LSIZE, z2).endVertex();
        worldRenderer.pos(x2 - LSIZE, y2 + LSIZE, z2).endVertex();
        worldRenderer.pos(x1 - 0.01F, y1 + 0.01F, z1).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1 + 0.01F, y1 - 0.01F, z1).endVertex();
        worldRenderer.pos(x2 + LSIZE, y2 - LSIZE, z2).endVertex();
        worldRenderer.pos(x2 + LSIZE, y2 + LSIZE, z2).endVertex();
        worldRenderer.pos(x1 + 0.01F, y1 + 0.01F, z1).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1 - 0.01F, y1 - 0.01F, z1).endVertex();
        worldRenderer.pos(x2 - LSIZE, y2 - LSIZE, z2).endVertex();
        worldRenderer.pos(x2 + LSIZE, y2 - LSIZE, z2).endVertex();
        worldRenderer.pos(x1 + 0.01F, y1 - 0.01F, z1).endVertex();
        tess.draw();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1 + 0.01F, z1 + 0.01F).endVertex();
        worldRenderer.pos(x2, y2 + LSIZE, z2 + LSIZE).endVertex();
        worldRenderer.pos(x2, y2 + LSIZE, z2 - LSIZE).endVertex();
        worldRenderer.pos(x1, y1 + 0.01F, z1 - 0.01F).endVertex();
        tess.draw();
    }

    private void renderLaserModel(float retraction)
    {
    	float laserretraction = retraction / 0.8F;
    	if (laserretraction > 1F) laserretraction = 1F;
    	float guardmovement = (retraction - 0.6F) / 0.4F * 1.875F;
    	if (guardmovement < 0F) guardmovement = 0F;
    	GL11.glPushMatrix();
    	float zadjust = laserretraction * 5F;
    	float yadjust = zadjust;

    	if (yadjust > 0.938F)
    	{
    		yadjust = 0.938F;
    		zadjust = (zadjust - yadjust) * 2.5F + yadjust;
    	}
        GL11.glTranslatef(0F, yadjust, zadjust);
        this.drawBakedModel(modellaser1);
	    if (yadjust == 0.938F)
	    {
	        //Do not move laser centre into body
	    	GL11.glTranslatef(0F, 0F, -zadjust + 0.938F);
	    }
        this.drawBakedModel(modellaser3);
    	GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glTranslatef(guardmovement, 0F, 0F);
        this.drawBakedModel(modellasergl);
        GL11.glTranslatef(-2 * guardmovement + 8.75F, 0F, 0F);
        this.drawBakedModel(modellasergl);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAstroMiner entity)
    {
        return new ResourceLocation("missing");
    }

    private void drawBakedModel(IFlexibleBakedModel model)
    {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, model.getFormat());

        for (BakedQuad bakedquad : model.getGeneralQuads())
        {
            LightUtil.renderQuadColor(worldrenderer, bakedquad, -1);
        }

        tessellator.draw();
    }
}
