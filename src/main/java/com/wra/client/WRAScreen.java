package com.wra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Clickable GUI; RSHIFT and ESC close, left-click toggles modules. */
public class WRAScreen extends Screen {
    private int category = 0;
    private int left, top, panelWidth, panelHeight;
    private final int cyan = 0xFF55EBD8;

    public WRAScreen() { super(Component.literal("WRA Client")); }
    @Override protected void init() { layout(); }
    private void layout() {
        panelWidth = Math.min(600, Math.max(250, this.width - 22));
        panelHeight = Math.min(362, Math.max(225, this.height - 22));
        left = (this.width - panelWidth) / 2;
        top = (this.height - panelHeight) / 2;
    }

    private void rect(GuiGraphics g, int x, int y, int w, int h, int color) { g.fill(x, y, x+w, y+h, color); }
    private void rounded(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        rect(g,x+radius,y,w-2*radius,h,color);
        rect(g,x,y+radius,radius,h-2*radius,color);
        rect(g,x+w-radius,y+radius,radius,h-2*radius,color);
        for (int i = 0; i < radius; i++) {
            int inset = (int)Math.ceil(radius - Math.sqrt(Math.max(0, radius*radius - (radius-i-0.5)*(radius-i-0.5)));
            rect(g,x+inset,y+i,w-inset*2,1,color);
            rect(g,x+inset,y+h-i-1,w-inset*2,1,color);
        }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        layout();
        g.fillGradient(0, 0, width, height, 0xDD050A15, 0xED0B1428);
        rounded(g,left-2,top-2,panelWidth+4,panelHeight+4,12,0xFF24344D);
        rounded(g,left,top,panelWidth,panelHeight,12,0xFF0E1729);
        g.fillGradient(left+10, top+10, left+panelWidth-10, top+13, 0xFF42E9D9, 0xFF9965F4);
        g.drawString(font,"WRA",left+20,top+26,cyan,false);
        g.drawString(font,"CLIENT",left+49,top+26,0xFFF5F7FF,false);
        g.drawString(font,"FORGE 1.20.1  /  V1.0",left+20,top+43,0xFF8C9EB8,false);
        String hint = "RSHIFT  CLOSE";
        g.drawString(font,hint,left+panelWidth-20-font.width(hint),top+29,0xFF97ADC4,false);

        int tabGap = 4, tabW = (panelWidth-40-tabGap*4)/5;
        for (int i=0; i<5; i++) {
            int tx=left+20+i*(tabW+tabGap), ty=top+69;
            boolean hovered=inside(mouseX,mouseY,tx,ty,tabW,28);
            rounded(g,tx,ty,tabW,28,6,i==category?0xFF226B77:(hovered?0xFF24354E:0xFF17253A));
            String label=ClientFeatures.CATEGORIES[i];
            g.drawCenteredString(font,label,tx+tabW/2,ty+10,i==category?0xFFE8FFFA:0xFFA8B9D1);
        }
        g.drawString(font,"MODULES",left+22,top+114,0xFFE4EFFF,false);
        g.drawString(font,"LEFT CLICK TO TOGGLE",left+panelWidth-22-font.width("LEFT CLICK TO TOGGLE"),top+114,0xFF7289A4,false);

        int gap=12, inner=panelWidth-44;
        int colW=(inner-gap)/2;
        List<ClientFeatures.Feature> modules=ClientFeatures.FEATURES.stream().filter(f->f.category==category).toList();
        for(int i=0;i<modules.size();i++) {
            ClientFeatures.Feature f=modules.get(i);
            int col=i%2,row=i/2;
            int x=left+22+col*(colW+gap),y=top+134+row*68;
            boolean hover=inside(mouseX,mouseY,x,y,colW,57);
            rounded(g,x,y,colW,57,7,hover?0xFF22344C:0xFF18273A);
            rect(g,x,y+10,3,37,f.enabled?cyan:0xFF51637D);
            g.drawString(font,f.name,x+13,y+11,0xFFF3F9FF,false);
            String description=f.description;
            int allowed=Math.max(9,colW-75);
            while(font.width(description)>allowed && description.length()>3) description=description.substring(0,description.length()-4)+"...";
            g.drawString(font,description,x+13,y+29,0xFF8EA4BB,false);
            rounded(g,x+colW-43,y+11,32,17,8,f.enabled?0xFF238E7F:0xFF41506B);
            rounded(g,x+colW-(f.enabled?24:40),y+13,13,13,6,0xFFF4FAFF);
        }
        String footer="20 MODULES  •  5 CATEGORIES  •  MOUSE CONTROL";
        if (width > 400) g.drawCenteredString(font,footer,left+panelWidth/2,top+panelHeight-17,0xFF627D9A);
    }
    private boolean inside(double mx,double my,int x,int y,int w,int h) { return mx>=x && mx<x+w && my>=y && my<y+h; }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(button!=GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mx,my,button);
        int gap=4,tabW=(panelWidth-40-gap*4)/5;
        for(int i=0;i<5;i++) if(inside(mx,my,left+20+i*(tabW+gap),top+69,tabW,28)) { category=i; return true; }
        List<ClientFeatures.Feature> modules=ClientFeatures.FEATURES.stream().filter(f->f.category==category).toList();
        int cardGap=12,colW=(panelWidth-44-cardGap)/2;
        for(int i=0;i<modules.size();i++) {
            int x=left+22+(i%2)*(colW+cardGap),y=top+134+(i/2)*68;
            if(inside(mx,my,x,y,colW,57)) { ClientFeatures.toggle(modules.get(i)); return true; }
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT || key==GLFW.GLFW_KEY_ESCAPE) { Minecraft.getInstance().setScreen(null); return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
