package pl.poznan.put.cs.bioserver.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import pl.poznan.put.cs.bioserver.external.Matplotlib.Method;
import pl.poznan.put.cs.bioserver.helper.Visualizable;

@XmlRootElement
public class ClusteringHierarchical extends XMLSerializable implements
        Visualizable {
    private static final long serialVersionUID = -933748828643803893L;

    ComparisonGlobal comparison;
    Method method;

    public static ClusteringHierarchical newInstance(
            ComparisonGlobal comparison, Method method) {
        ClusteringHierarchical instance = new ClusteringHierarchical();
        instance.setComparison(comparison);
        instance.setMethod(method);
        return instance;
    }

    public ComparisonGlobal getComparison() {
        return comparison;
    }

    @XmlElement
    public void setComparison(ComparisonGlobal comparison) {
        this.comparison = comparison;
    }

    public Method getMethod() {
        return method;
    }

    @XmlElement
    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public void visualize() {
        // TODO Auto-generated method stub

    }

    @Override
    public void visualizeHighQuality() {
        // TODO Auto-generated method stub

    }
}
