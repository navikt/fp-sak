package no.nav.foreldrepenger.datavarehus.task;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.datavarehus.domene.AksjonspunktDefDvh;
import no.nav.foreldrepenger.datavarehus.domene.DatavarehusRepository;

/**
 *  Batchservice som finner alle behandlinger som skal gjenopptas, og lager en ditto prosess task for hver.
 *  Kriterier for gjenopptagelse: Behandlingen har et åpent aksjonspunkt som er et autopunkt og
 *  har en frist som er passert.
 */
@ApplicationScoped
public class OppdaterAksjonspunktDefinisjonBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAME = "BVL010";
    private static final String EXECUTION_ID_SEPARATOR = "-";

    private DatavarehusRepository datavarehusRepository;

    @Inject
    public OppdaterAksjonspunktDefinisjonBatchTjeneste(DatavarehusRepository datavarehusRepository) {
        this.datavarehusRepository = datavarehusRepository;
    }

    @Override
    public String launch(Properties properties) {
        oppdaterAksjonspunktDefinisjonDVH();
        return BATCHNAME + EXECUTION_ID_SEPARATOR + LocalDate.now().toString();
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    private void oppdaterAksjonspunktDefinisjonDVH() {
        var eksisterende = datavarehusRepository.hentAksjonspunktDefinisjoner();
        AksjonspunktDefinisjon.kodeMap().values().stream().filter(ad -> ad.getKode() != null).forEach(ad -> {
            if (eksisterende.get(ad.getKode()) == null) {
                var defDvh = AksjonspunktDefDvh.builder()
                    .aksjonspunktDef(ad.getKode())
                    .aksjonspunktType(ad.getAksjonspunktType().getKode())
                    .aksjonspunktNavn(ad.getNavn())
                    .build();
                datavarehusRepository.lagre(defDvh);
            } else {
                var dvhDefinisjon = eksisterende.get(ad.getKode());
                if (!Objects.equals(ad.getNavn(), dvhDefinisjon.getAksjonspunktNavn())) {
                    dvhDefinisjon.setAksjonspunktNavn(ad.getNavn());
                    datavarehusRepository.lagre(dvhDefinisjon);
                }
            }
        });
    }
}
