package no.nav.foreldrepenger.mottak.hendelser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødForretningshendelse;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødfødselForretningshendelse;
import no.nav.foreldrepenger.familiehendelse.fødsel.FødselForretningshendelse;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødfødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.FødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.DødForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.DødfødselForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.ForretningshendelseSaksvelgerProvider;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.FødselForretningshendelseSaksvelger;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class KlargjørHendelseTaskTest {

    @Test
    public void skal_kalle_videre_på_domenetjeneste() throws Exception {
        ForretningshendelseMottak domenetjeneste = mock(ForretningshendelseMottak.class);
        KlargjørHendelseTask task = new KlargjørHendelseTask(domenetjeneste);

        ProsessTaskData taskData = new ProsessTaskData(KlargjørHendelseTask.TASKTYPE);
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, "FØDSEL");
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, "id_1");
        var hendelse = new FødselHendelseDto();
        hendelse.setId("id_1");
        hendelse.setAktørIdForeldre(Collections.singletonList(AktørId.dummy().getId()));
        hendelse.setFødselsdato(LocalDate.now());
        taskData.setPayload(JsonMapper.toJson(hendelse));

        ArgumentCaptor<ForretningshendelseType> captorT = ArgumentCaptor.forClass(ForretningshendelseType.class);
        ArgumentCaptor<HendelseDto> captorP = ArgumentCaptor.forClass(HendelseDto.class);

        task.doTask(taskData);

        Mockito.verify(domenetjeneste).mottaForretningshendelse(captorT.capture(), captorP.capture());
        assertThat(captorT.getValue()).isEqualTo(ForretningshendelseType.FØDSEL);
        assertThat(captorP.getValue()).isInstanceOf(FødselHendelseDto.class);
    }

    @Test
    public void skal_motta_fødsel() throws Exception {
        AktørId aktørId = AktørId.dummy();
        ForretningshendelseSaksvelgerProvider saksvelgerProvider = mock(ForretningshendelseSaksvelgerProvider.class);
        ForretningshendelseSaksvelger saksvelger = mock(FødselForretningshendelseSaksvelger.class);
        when(saksvelger.finnRelaterteFagsaker(any())).thenReturn(new LinkedHashMap<BehandlingÅrsakType, List<Fagsak>>());
        when(saksvelgerProvider.finnSaksvelger(ForretningshendelseType.FØDSEL)).thenReturn(saksvelger);
        ForretningshendelseMottak domenetjeneste = new ForretningshendelseMottak(null,
            saksvelgerProvider, mock(BehandlingRepositoryProvider.class), mock(ProsessTaskRepository.class), null);

        KlargjørHendelseTask task = new KlargjørHendelseTask(domenetjeneste);

        ProsessTaskData taskData = new ProsessTaskData(KlargjørHendelseTask.TASKTYPE);
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, "FØDSEL");
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, "id_1");
        var hendelse = new FødselHendelseDto();
        hendelse.setId("id_1");
        hendelse.setAktørIdForeldre(Collections.singletonList(aktørId.getId()));
        hendelse.setFødselsdato(LocalDate.now());
        taskData.setPayload(JsonMapper.toJson(hendelse));

        ArgumentCaptor<Forretningshendelse> captor = ArgumentCaptor.forClass(Forretningshendelse.class);

        task.doTask(taskData);

        Mockito.verify(saksvelger).finnRelaterteFagsaker(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(FødselForretningshendelse.class);
        assertThat((captor.getValue()).getAktørIdListe()).containsExactly(aktørId);
        assertThat(((FødselForretningshendelse)captor.getValue()).getFødselsdato()).isEqualTo(LocalDate.now());
    }


    @Test
    public void skal_motta_dødfødsel() throws Exception {
        ForretningshendelseSaksvelgerProvider saksvelgerProvider = mock(ForretningshendelseSaksvelgerProvider.class);
        ForretningshendelseSaksvelger saksvelger = mock(DødfødselForretningshendelseSaksvelger.class);
        when(saksvelger.finnRelaterteFagsaker(any())).thenReturn(new LinkedHashMap<BehandlingÅrsakType, List<Fagsak>>());
        when(saksvelgerProvider.finnSaksvelger(ForretningshendelseType.DØDFØDSEL)).thenReturn(saksvelger);
        ForretningshendelseMottak domenetjeneste = new ForretningshendelseMottak(null,
            saksvelgerProvider, mock(BehandlingRepositoryProvider.class), mock(ProsessTaskRepository.class), null);

        KlargjørHendelseTask task = new KlargjørHendelseTask(domenetjeneste);

        ProsessTaskData taskData = new ProsessTaskData(KlargjørHendelseTask.TASKTYPE);
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, "DØDFØDSEL");
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, "id_1");
        var hendelse = new DødfødselHendelseDto();
        hendelse.setId("id_1");
        hendelse.setAktørId(Collections.singletonList(AktørId.dummy().getId()));
        hendelse.setDødfødselsdato(LocalDate.now());
        taskData.setPayload(JsonMapper.toJson(hendelse));

        ArgumentCaptor<Forretningshendelse> captor = ArgumentCaptor.forClass(Forretningshendelse.class);

        task.doTask(taskData);

        Mockito.verify(saksvelger).finnRelaterteFagsaker(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DødfødselForretningshendelse.class);
        assertThat(((DødfødselForretningshendelse)captor.getValue()).getDødfødselsdato()).isEqualTo(LocalDate.now());
    }

    @Test
    public void skal_motta_død() throws Exception {
        ForretningshendelseSaksvelgerProvider saksvelgerProvider = mock(ForretningshendelseSaksvelgerProvider.class);
        ForretningshendelseSaksvelger saksvelger = mock(DødForretningshendelseSaksvelger.class);
        when(saksvelger.finnRelaterteFagsaker(any())).thenReturn(new LinkedHashMap<BehandlingÅrsakType, List<Fagsak>>());
        when(saksvelgerProvider.finnSaksvelger(ForretningshendelseType.DØD)).thenReturn(saksvelger);
        ForretningshendelseMottak domenetjeneste = new ForretningshendelseMottak(null,
            saksvelgerProvider, mock(BehandlingRepositoryProvider.class), mock(ProsessTaskRepository.class), null);

        KlargjørHendelseTask task = new KlargjørHendelseTask(domenetjeneste);

        ProsessTaskData taskData = new ProsessTaskData(KlargjørHendelseTask.TASKTYPE);
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, "DØD");
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, "id_1");
        var hendelse = new DødHendelseDto();
        hendelse.setId("id_1");
        hendelse.setAktørId(Collections.singletonList(AktørId.dummy().getId()));
        hendelse.setDødsdato(LocalDate.now());
        taskData.setPayload(JsonMapper.toJson(hendelse));

        ArgumentCaptor<Forretningshendelse> captor = ArgumentCaptor.forClass(Forretningshendelse.class);

        task.doTask(taskData);

        Mockito.verify(saksvelger).finnRelaterteFagsaker(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DødForretningshendelse.class);
        assertThat(((DødForretningshendelse)captor.getValue()).getDødsdato()).isEqualTo(LocalDate.now());
    }
}
