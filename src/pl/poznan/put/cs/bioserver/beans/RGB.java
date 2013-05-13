package pl.poznan.put.cs.bioserver.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RGB extends XMLSerializable {
    private static final long serialVersionUID = 9081446619961449105L;

    float r;
    float g;
    float b;

    public static RGB newInstance(float[] rgb) {
        RGB instance = new RGB();
        instance.r = rgb[0];
        instance.g = rgb[1];
        instance.b = rgb[2];
        return instance;
    }

    public float getR() {
        return r;
    }

    @XmlElement
    public void setR(float r) {
        this.r = r;
    }

    public float getG() {
        return g;
    }

    @XmlElement
    public void setG(float g) {
        this.g = g;
    }

    public float getB() {
        return b;
    }

    @XmlElement
    public void setB(float b) {
        this.b = b;
    }

}
