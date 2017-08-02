package roborio.learning;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.InvalidConfigurationException;
import org.jgap.supergenes.AbstractSupergene;
import org.jgap.supergenes.Supergene;
import roborio.learning.functions.IdentityFunction;
import roborio.learning.functions.InverseFunction;
import roborio.learning.functions.NormFunction;
import roborio.learning.functions.PowerFunction;

/**
 * Created by Roberto Sales on 01/08/17.
 */
public class DCFunctionGene extends AbstractSupergene {
    private static final Class<? extends NormFunction>[] FUNCTIONS =
            new Class[]{IdentityFunction.class, InverseFunction.class, PowerFunction.class};

    public DCFunctionGene() throws InvalidConfigurationException {
    }

    public DCFunctionGene(Configuration a_config) throws InvalidConfigurationException {
        super(a_config);
    }

    public DCFunctionGene(Configuration a_conf, Gene[] a_genes) throws InvalidConfigurationException {
        super(a_conf, a_genes);
    }

    @Override
    public boolean isValid (Gene [] genes, Supergene supergene) {
        int fnId = (Integer) geneAt(0).getAllele();
        double[] p = new double[getGenes().length - 1];
        for(int i = 0; i < p.length; i++) {
            p[i] = (Double) geneAt(i+1).getAllele();
        }

        return true;
    }

    public Class<? extends NormFunction> getFunction() {
        return FUNCTIONS[(Integer) geneAt(0).getAllele()];
    }

    public double[] getParams() {
        double[] p = new double[getGenes().length - 1];
        for(int i = 0; i < p.length; i++) {
            p[i] = (Double) geneAt(i+1).getAllele();
        }

        return p;
    }

    public static int possibilities() {
        return FUNCTIONS.length;
    }
}
