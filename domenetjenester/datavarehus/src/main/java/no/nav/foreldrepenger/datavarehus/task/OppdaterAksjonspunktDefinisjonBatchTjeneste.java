package no.nav.foreldrepenger.datavarehus.task;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.LagretKodeverdiNavn;
import no.nav.foreldrepenger.behandlingslager.kodeverk.LagretKodeverdiRepository;

/**
 *  Batchservice som lagrer ned kodeverdier med tilhørende navn slik at navn kan brukes i db-spørringer
 */
@ApplicationScoped
public class OppdaterAksjonspunktDefinisjonBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAME = "BVL010";
    private static final String EXECUTION_ID_SEPARATOR = "-";
    private static final String KODEVERK_AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.KODEVERK;

    private final LagretKodeverdiRepository lagretKodeverdiRepository;

    @Inject
    public OppdaterAksjonspunktDefinisjonBatchTjeneste(LagretKodeverdiRepository lagretKodeverdiRepository) {
        this.lagretKodeverdiRepository = lagretKodeverdiRepository;
    }

    @Override
    public String launch(Properties properties) {
        oppdaterKodeverk(KODEVERK_AKSJONSPUNKT_DEF, Arrays.stream(AksjonspunktDefinisjon.values()));
        return BATCHNAME + EXECUTION_ID_SEPARATOR + LocalDate.now();
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    private void oppdaterKodeverk(String kodeverk, Stream<? extends Kodeverdi> verdier) {
        var eksisterende = lagretKodeverdiRepository.hentLagretKodeverk(kodeverk);
        verdier.filter(ad -> ad.getKode() != null).forEach(ad -> {
            if (eksisterende.get(ad.getKode()) == null) {
                var kodeverdi = LagretKodeverdiNavn.builder()
                    .kodeverk(kodeverk)
                    .kode(ad.getKode())
                    .navn(ad.getNavn())
                    .build();
                lagretKodeverdiRepository.lagre(kodeverdi);
            } else {
                var kodeverdi = eksisterende.get(ad.getKode());
                if (!Objects.equals(ad.getNavn(), kodeverdi.getNavn())) {
                    kodeverdi.setNavn(ad.getNavn());
                    lagretKodeverdiRepository.lagre(kodeverdi);
                }
            }
        });
    }
}
