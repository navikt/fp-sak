package no.nav.foreldrepenger.domene.medlem.medl2;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class HentMedlemskapFraRegister {

    private static final Logger LOG = LoggerFactory.getLogger(HentMedlemskapFraRegister.class);

    private Medlemskap restKlient;
    private MedlemsperioderRestKlient medlemsperioderRestKlient;

    HentMedlemskapFraRegister() {
        // CDI
    }

    @Inject
    public HentMedlemskapFraRegister(Medlemskap restKlient, MedlemsperioderRestKlient medlemsperioderRestKlient) {
        this.restKlient = restKlient;
        this.medlemsperioderRestKlient = medlemsperioderRestKlient;
    }

    public List<Medlemskapsperiode> finnMedlemskapPerioder(AktørId aktørId, LocalDate fom, LocalDate tom) {
        var mups = restKlient.finnMedlemsunntak(aktørId.getId(), fom, tom);
        LOG.info("MEDL2 REST RS {}", mups);
        sammenlign(mups, aktørId, fom, tom);
        return mups.stream().map(this::mapFraMedlemsunntak).toList();
    }

    private void sammenlign(List<Medlemskapsunntak> input, AktørId aktørId, LocalDate fom, LocalDate tom) {
        try {
            var perioder = medlemsperioderRestKlient.finnMedlemsunntak(aktørId.getId(), fom, tom);
            if (perioder.size() == input.size() && perioder.containsAll(input)) {
                LOG.info("MEDL2 POST sammenligning OK");
            } else {
                LOG.info("MEDL2 POST sammenligning ulike gammel {} ny {}", input, perioder);
            }
        } catch (Exception e) {
            LOG.info("MEDL2 POST sammenligning feil", e);
        }
    }

    private Medlemskapsperiode mapFraMedlemsunntak(Medlemskapsunntak medlemsperiode) {
        return new Medlemskapsperiode.Builder()
            .medFom(medlemsperiode.fraOgMed())
            .medTom(medlemsperiode.tilOgMed())
            .medDatoBesluttet(medlemsperiode.getBesluttet())
            .medErMedlem(medlemsperiode.medlem())
            .medKilde(mapTilKilde(medlemsperiode.getKilde()))
            .medDekning(mapTilDekning(medlemsperiode.dekning()))
            .medLovvalg(mapTilLovvalg(medlemsperiode.lovvalg()))
            .medLovvalgsland(finnLand(medlemsperiode.lovvalgsland()))
            .medStudieland(finnLand(medlemsperiode.getStudieland()))
            .medMedlId(medlemsperiode.unntakId())
            .build();
    }

    private Landkoder finnLand(String land) {
        if (land != null) {
            return Landkoder.fraKode(land);
        }
        return null;
    }

    private MedlemskapDekningType mapTilDekning(String trygdeDekning) {
        var dekningType = MedlemskapDekningType.UDEFINERT;
        if (trygdeDekning != null) {
            dekningType = MedlemskapsperiodeKoder.DEKNING_TYPE_MAP.get(trygdeDekning);
            if (dekningType == null) {
                dekningType = MedlemskapDekningType.UDEFINERT;
            }
        }
        return dekningType;
    }

    private MedlemskapType mapTilLovvalg(String lovvalg) {
        var medlemskapType = MedlemskapType.UDEFINERT;
        if (lovvalg != null) {
            if (MedlemskapsperiodeKoder.Lovvalg.ENDL.name().compareTo(lovvalg) == 0) {
                medlemskapType = MedlemskapType.ENDELIG;
            }
            if (MedlemskapsperiodeKoder.Lovvalg.UAVK.name().compareTo(lovvalg) == 0) {
                medlemskapType = MedlemskapType.UNDER_AVKLARING;
            }
            if (MedlemskapsperiodeKoder.Lovvalg.FORL.name().compareTo(lovvalg) == 0) {
                medlemskapType = MedlemskapType.FORELOPIG;
            }
        }
        return medlemskapType;
    }

    private MedlemskapKildeType mapTilKilde(String kilde) {
        var kildeType = MedlemskapKildeType.UDEFINERT;
        if (kilde != null) {
            kildeType = MedlemskapKildeType.fraKode(kilde);
            if (kildeType == null) {
                kildeType = MedlemskapKildeType.ANNEN;
            }
            if (MedlemskapKildeType.SRVGOSYS.equals(kildeType)) {
                kildeType = MedlemskapKildeType.FS22;
            }
            if (MedlemskapKildeType.SRVMELOSYS.equals(kildeType)) {
                kildeType = MedlemskapKildeType.MEDL;
            }
        }
        return kildeType;
    }
}
