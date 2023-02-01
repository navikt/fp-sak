package no.nav.foreldrepenger.mottak.hendelser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

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
        var hendelse = DefaultJsonMapper.fromJson(prosessTaskData.getPayloadAsString(), HendelseDto.class);
        forretningshendelseMottak.mottaForretningshendelse(hendelseType, hendelse);
    }
}
