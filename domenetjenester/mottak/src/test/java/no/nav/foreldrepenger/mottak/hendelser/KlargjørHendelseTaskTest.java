package no.nav.foreldrepenger.mottak.hendelser;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseMottak;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.KlargjørHendelseTask;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.FødselHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class KlargjørHendelseTaskTest {

    @Test
    public void skal_kalle_videre_på_domenetjeneste() throws Exception {
        ForretningshendelseMottak domenetjeneste = Mockito.mock(ForretningshendelseMottak.class);
        KlargjørHendelseTask task = new KlargjørHendelseTask(domenetjeneste);

        ProsessTaskData taskData = new ProsessTaskData(KlargjørHendelseTask.TASKTYPE);
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, "FØDSEL");
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, "id_1");
        taskData.setPayload(JsonMapper.toJson(new FødselHendelse(Collections.singletonList(AktørId.dummy().getId()), LocalDate.now())));

        task.doTask(taskData);

        Mockito.verify(domenetjeneste).mottaForretningshendelse(Mockito.any(ForretningshendelseDto.class));
    }
}
