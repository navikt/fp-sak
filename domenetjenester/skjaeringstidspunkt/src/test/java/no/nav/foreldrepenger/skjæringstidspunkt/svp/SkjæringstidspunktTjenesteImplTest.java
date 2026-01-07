package no.nav.foreldrepenger.skjæringstidspunkt.svp;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ExtendWith(MockitoExtension.class)
class SkjæringstidspunktTjenesteImplTest {
    @Mock
    private SvangerskapspengerRepository svangerskapspengerRepository;
    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private OpptjeningRepository opptjeningRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjenesteImpl skjæringstidspunktTjeneste;

    @BeforeEach
    void setUp() {
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(svangerskapspengerRepository, beregningsresultatRepository, familieHendelseRepository, opptjeningRepository, behandlingRepository);
    }

    @Test
    void skal_utlede_skjæringstidspunktet() {
        var forventetSkjæringstidspunkt = LocalDate.of(2019, 7, 10);

        var svpGrunnlagEntitet = new SvpGrunnlagEntitet.Builder();
        var svp = new SvpTilretteleggingEntitet.Builder();
        svp.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        svp.medBehovForTilretteleggingFom(forventetSkjæringstidspunkt);
        svp.medDelvisTilrettelegging(forventetSkjæringstidspunkt, BigDecimal.valueOf(50), forventetSkjæringstidspunkt, no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde.SØKNAD);
        svp.medDelvisTilrettelegging(LocalDate.of(2019, 9, 17), BigDecimal.valueOf(30), forventetSkjæringstidspunkt, no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde.SØKNAD);
        svp.medHelTilrettelegging(LocalDate.of(2019, 11, 1), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD);
        svp.medIngenTilrettelegging(LocalDate.of(2019, 11, 25), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD);

        var tilretteleggingEntitet = svp.build();
        svpGrunnlagEntitet.medOpprinneligeTilrettelegginger(List.of(tilretteleggingEntitet));
        svpGrunnlagEntitet.medBehandlingId(1337L);

        var dag = SkjæringstidspunktTjenesteImpl.utledBasertPåGrunnlag(svpGrunnlagEntitet.build());

        assertThat(dag).isEqualTo(forventetSkjæringstidspunkt);
    }

    @Test
    void skal_utlede_nytt_stp_når_ny_uttaksdato_svp() {
        var stp = LocalDate.now().minusWeeks(2);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("987654321");
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurdering.medSøknadHendelse()
            .medTerminbekreftelse(revurdering.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(2)));
        revurdering.medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));

        var behandling = revurdering.lagMocked();
        behandling.avsluttBehandling();
        var behandlingId =  behandling.getId();

        var nySøknadsdato = stp.minusWeeks(3);
        var tilrFomArbeidsforhold1 = lagTilretteleggingFom(nySøknadsdato, TilretteleggingType.INGEN_TILRETTELEGGING);
        var tilretteleggingEntitet = lagTilrettelegging(arbeidsgiver, null, nySøknadsdato, List.of(tilrFomArbeidsforhold1));

        var tilrFomArbeidsforhold2 = lagTilretteleggingFom(stp, TilretteleggingType.INGEN_TILRETTELEGGING);
        var tilretteleggingEntitetArbeidsforhold2 = lagTilrettelegging(arbeidsgiver2, null, stp, List.of(tilrFomArbeidsforhold2));
        var svpGrunnlag = byggSvangerskapspengerGrunnlag(List.of(tilretteleggingEntitet, tilretteleggingEntitetArbeidsforhold2), behandlingId);

        when(svangerskapspengerRepository.hentGrunnlag(behandlingId)).thenReturn(Optional.of(svpGrunnlag));
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);
        when(opptjeningRepository.finnOpptjening(behandlingId)).thenReturn(Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)).thenReturn(Optional.empty());

        var resultat = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        assertThat(resultat.getUtledetSkjæringstidspunkt()).isEqualTo(nySøknadsdato);
    }

    private TilretteleggingFOM lagTilretteleggingFom(LocalDate startdato, TilretteleggingType type) {
        return new TilretteleggingFOM.Builder()
            .medTilretteleggingType(type)
            .medFomDato(startdato)
            .build();
    }
    private SvpTilretteleggingEntitet lagTilrettelegging(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, LocalDate stp, List<TilretteleggingFOM> fraDatoer) {
        var tilrBuilder = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medBehovForTilretteleggingFom(stp);
        if (ref != null){
            tilrBuilder.medInternArbeidsforholdRef(ref).build();
        }
        fraDatoer.forEach(tilrBuilder::medTilretteleggingFom);
        return tilrBuilder.build();
    }

    private SvpGrunnlagEntitet byggSvangerskapspengerGrunnlag(List<SvpTilretteleggingEntitet> tilretteleggingEntiteter, Long behandlingId) {
        return new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(tilretteleggingEntiteter)
            .medBehandlingId(behandlingId)
            .build();
    }
}
