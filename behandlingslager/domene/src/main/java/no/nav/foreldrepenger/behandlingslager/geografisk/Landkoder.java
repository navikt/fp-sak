package no.nav.foreldrepenger.behandlingslager.geografisk;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeliste;

@Entity(name = "Landkoder")
@DiscriminatorValue(Landkoder.DISCRIMINATOR)
public class Landkoder extends Kodeliste {

    public static final String DISCRIMINATOR = "LANDKODER";

    public static final Landkoder UDEFINERT = new Landkoder("-"); //$NON-NLS-1$

    public static final Landkoder NOR = new Landkoder("NOR"); //$NON-NLS-1$
    public static final Landkoder SWE = new Landkoder("SWE"); //$NON-NLS-1$
    public static final Landkoder DNK = new Landkoder("DNK"); //$NON-NLS-1$
    public static final Landkoder FIN = new Landkoder("FIN"); //$NON-NLS-1$
    public static final Landkoder ISL = new Landkoder("ISL"); //$NON-NLS-1$
    public static final Landkoder ALA = new Landkoder("ALA"); //$NON-NLS-1$
    public static final Landkoder FRO = new Landkoder("FRO"); //$NON-NLS-1$
    public static final Landkoder GRL = new Landkoder("GRL"); //$NON-NLS-1$

    public static final Landkoder USA = new Landkoder("USA"); //$NON-NLS-1$
    public static final Landkoder PNG = new Landkoder("PNG"); //$NON-NLS-1$
    public static final Landkoder CAN = new Landkoder("CAN"); //$NON-NLS-1$

    public static final Landkoder AUT = new Landkoder("AUT"); //$NON-NLS-1$
    public static final Landkoder BEL = new Landkoder("BEL"); //$NON-NLS-1$
    public static final Landkoder BGR = new Landkoder("BGR"); //$NON-NLS-1$
    public static final Landkoder CYP = new Landkoder("CYP"); //$NON-NLS-1$
    public static final Landkoder CZE = new Landkoder("CZE"); //$NON-NLS-1$
    public static final Landkoder DEU = new Landkoder("DEU"); //$NON-NLS-1$
    public static final Landkoder ESP = new Landkoder("ESP"); //$NON-NLS-1$
    public static final Landkoder EST = new Landkoder("EST"); //$NON-NLS-1$
    public static final Landkoder FRA = new Landkoder("FRA"); //$NON-NLS-1$
    public static final Landkoder GBR = new Landkoder("GBR"); //$NON-NLS-1$
    public static final Landkoder GRC = new Landkoder("GRC"); //$NON-NLS-1$
    public static final Landkoder HRV = new Landkoder("HRV"); //$NON-NLS-1$
    public static final Landkoder HUN = new Landkoder("HUN"); //$NON-NLS-1$
    public static final Landkoder IRL = new Landkoder("IRL"); //$NON-NLS-1$
    public static final Landkoder ITA = new Landkoder("ITA"); //$NON-NLS-1$
    public static final Landkoder LIE = new Landkoder("LIE"); //$NON-NLS-1$
    public static final Landkoder LTU = new Landkoder("LTU"); //$NON-NLS-1$
    public static final Landkoder LUX = new Landkoder("LUX"); //$NON-NLS-1$
    public static final Landkoder LVA = new Landkoder("LVA"); //$NON-NLS-1$
    public static final Landkoder MLT = new Landkoder("MLT"); //$NON-NLS-1$
    public static final Landkoder NLD = new Landkoder("NLD"); //$NON-NLS-1$
    public static final Landkoder POL = new Landkoder("POL"); //$NON-NLS-1$
    public static final Landkoder PRT = new Landkoder("PRT"); //$NON-NLS-1$
    public static final Landkoder ROU = new Landkoder("ROU"); //$NON-NLS-1$
    public static final Landkoder SVK = new Landkoder("SVK"); //$NON-NLS-1$
    public static final Landkoder SVN = new Landkoder("SVN"); //$NON-NLS-1$


    Landkoder() {
        // Hibernate trenger en
    }

    public Landkoder(String kode) {
        super(kode, DISCRIMINATOR);
    }

    public static boolean erNorge(String kode) {
        return NOR.getKode().equals(kode);
    }
}
