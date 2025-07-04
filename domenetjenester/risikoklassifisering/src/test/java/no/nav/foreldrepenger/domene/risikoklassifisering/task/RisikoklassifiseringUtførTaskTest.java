package no.nav.foreldrepenger.domene.risikoklassifisering.task;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.Saksnummer;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.YtelseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.AnnenPartDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingRequestDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class RisikoklassifiseringUtførTaskTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final Long BEHANDLING_ID = 123342L;
    private static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();

    private RisikoklassifiseringUtførTask risikoklassifiseringUtførTask;

    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Mock
    private RisikovurderingTjeneste risikovurderingTjeneste;

    @Mock
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    @Mock
    private PersonopplysningRepository personopplysningRepository;

    @Mock
    private BehandlingRepository behandlingRepository;


    private Behandling behandling;

    @BeforeEach
    void init(){
        risikoklassifiseringUtførTask = new RisikoklassifiseringUtførTask(risikovurderingTjeneste, behandlingRepository, skjæringstidspunktTjeneste,
            opplysningsPeriodeTjeneste, personopplysningRepository);
    }

    @Test
    void skal_produsere_melding_til_kafka() {
        lagBehandling();
        var ref = BehandlingReferanse.fra(behandling);
        forberedelse(ref, true);
        var prosessTaskData = ProsessTaskData.forProsessTask(RisikoklassifiseringUtførTask.class);
                prosessTaskData.setBehandling("9999", 0L, BEHANDLING_ID);
        risikoklassifiseringUtførTask.doTask(prosessTaskData);
        var request = new RisikovurderingRequestDto(
            new no.nav.foreldrepenger.kontrakter.risk.kodeverk.AktørId(ref.aktørId().getId()), SKJÆRINGSTIDSPUNKT, LocalDate.now(),
            LocalDate.now(), ref.behandlingUuid(), YtelseType.ENGANGSSTØNAD,
            new AnnenPartDto(new no.nav.foreldrepenger.kontrakter.risk.kodeverk.AktørId(ANNEN_PART_AKTØR_ID.getId()), null),
            new Saksnummer(ref.saksnummer().getVerdi()));
        verify(risikovurderingTjeneste).startRisikoklassifisering(ref, request);
    }

    private void forberedelse(BehandlingReferanse behandlingref, boolean annenPart) {
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();
        when(behandlingRepository.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingref.behandlingId())).thenReturn(skjæringstidspunkt);
        when(opplysningsPeriodeTjeneste.beregn(behandlingref.behandlingId(), behandlingref.fagsakYtelseType())).thenReturn(
            SimpleLocalDateInterval.fraOgMedTomNotNull(LocalDate.now(), LocalDate.now()));
        if (annenPart) {
            when(personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandlingref.behandlingId()))
                .thenReturn(Optional.of(new OppgittAnnenPartBuilder().medAktørId(ANNEN_PART_AKTØR_ID).build()));
        }
        MDC.put("callId", "callId");
    }

    private void lagBehandling() {
        var terminDato = LocalDate.now().minusDays(70);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medSøknadDato(terminDato.minusDays(20));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        behandling = scenario.lagMocked();
    }
}
