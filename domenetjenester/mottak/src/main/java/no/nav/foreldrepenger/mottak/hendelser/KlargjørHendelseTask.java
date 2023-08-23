package no.nav.foreldrepenger.mottak.hendelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hendelser.klargjoering")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class KlargjørHendelseTask implements ProsessTaskHandler {

    public static final String PROPERTY_HENDELSE_TYPE = "hendelseType";
    public static final String PROPERTY_UID = "hendelseUid";

    private ForretningshendelseMottak forretningshendelseMottak;

    KlargjørHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public KlargjørHendelseTask(ForretningshendelseMottak forretningshendelseMottak) {
        this.forretningshendelseMottak = forretningshendelseMottak;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var hendelseType = ForretningshendelseType.fraKode(prosessTaskData.getPropertyValue(PROPERTY_HENDELSE_TYPE));
        var hendelse = StandardJsonConfig.fromJson(prosessTaskData.getPayloadAsString(), HendelseDto.class);
        forretningshendelseMottak.mottaForretningshendelse(hendelseType, hendelse);
    }
}
