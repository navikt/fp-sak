package no.nav.foreldrepenger.domene.medlem.impl;

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
import no.nav.foreldrepenger.domene.medlem.api.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.medlem.api.MedlemskapsperiodeKoder;
import no.nav.foreldrepenger.domene.medlem.medl2.Medlemskap;
import no.nav.foreldrepenger.domene.medlem.medl2.Medlemskapsunntak;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.exception.IntegrasjonException;

@ApplicationScoped
public class HentMedlemskapFraRegister {

    private static final Logger LOG = LoggerFactory.getLogger(HentMedlemskapFraRegister.class);

    private Medlemskap restKlient;

    HentMedlemskapFraRegister() {
        // CDI
    }

    @Inject
    public HentMedlemskapFraRegister(Medlemskap restKlient) {
        this.restKlient = restKlient;
    }

    public List<Medlemskapsperiode> finnMedlemskapPerioder(AktørId aktørId, LocalDate fom, LocalDate tom) {
        try {
            var mups = restKlient.finnMedlemsunntak(aktørId.getId(), fom, tom).stream()
                .map(this::mapFraMedlemsunntak)
                .toList();
            LOG.info("MEDL2 REST RS {}", mups);
            return mups;
        } catch (Exception e) {
            throw new IntegrasjonException("FP-085791", "Feil ved kall til medlemskap tjenesten.", e);
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
