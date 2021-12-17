package no.nav.foreldrepenger.domene.risikoklassifisering.konsument;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.kontroll.v1.KontrollResultatV1;

@ProsessTask("risiko.klassifisering.resultat")
public class LesKontrollresultatTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LesKontrollresultatTask.class);

    private RisikovurderingTjeneste risikovurderingTjeneste;
    private KontrollresultatMapper kontrollresultatMapper;

    public LesKontrollresultatTask() {
    }

    @Inject
    public LesKontrollresultatTask(RisikovurderingTjeneste risikovurderingTjeneste,
                                   KontrollresultatMapper kontrollresultatMapper) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.kontrollresultatMapper = kontrollresultatMapper;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var payload = prosessTaskData.getPayloadAsString();
        try {
            var kontraktResultat = StandardJsonConfig.fromJson(payload, KontrollResultatV1.class);
            evaluerKontrollresultat(kontraktResultat);
        } catch (Exception e) {
            LOG.warn("Klarte ikke behandle risikoklassifiseringresultat", e);
        }
    }

    private void evaluerKontrollresultat(KontrollResultatV1 kontraktResultat) {
        var resultatWrapper = KontrollresultatMapper.fraKontrakt(kontraktResultat);
        risikovurderingTjeneste.lagreKontrollresultat(resultatWrapper);
    }


}
