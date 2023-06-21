package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@CdiDbAwareTest
class SvpDtoTjenesteTest {

    @Inject
    private SvpDtoTjeneste tjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void henter_sak_med_svangerskapspenger() {
        var vedtakstidspunkt = LocalDateTime.now();

        var fhBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        var termindato = LocalDate.of(2023, 10, 19);
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
            .medSøknadHendelse(fhBuilder.medTerminbekreftelse(fhBuilder.getTerminbekreftelseBuilder().medTermindato(termindato)));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);
        var mottattDokument = new MottattDokument.Builder().medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medDokumentType(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER)
            .medMottattTidspunkt(LocalDateTime.now().minusDays(5))
            .medJournalPostId(new JournalpostId(1L))
            .build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        var entityManager = repositoryProvider.getEntityManager();
        var svpRepo = new SvangerskapspengerRepository(entityManager);
        var stillingsprosent = BigDecimal.valueOf(50);
        var tl = new SvpTilretteleggingEntitet.Builder()
            .medArbeidType(ArbeidType.FRILANSER)
            .medDelvisTilrettelegging(termindato.minusWeeks(10), stillingsprosent, termindato.minusWeeks(10))
            .medOpplysningerOmRisikofaktorer("risk!")
            .medOpplysningerOmTilretteleggingstiltak("gjort tiltak!")
            .medBehovForTilretteleggingFom(termindato.minusWeeks(12))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .build();
        svpRepo.lagreOgFlush(new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tl))
            .build());
        var uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(termindato.minusWeeks(10),
            termindato.minusWeeks(3).minusDays(1)).medUtbetalingsgrad(new Utbetalingsgrad(80))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak._8314)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        var uttak = new SvangerskapspengerUttakResultatEntitet.Builder(behandlingsresultat)
            .medUttakResultatArbeidsforhold(new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS)
                .medPeriode(uttakPeriode)
                .build()).build();
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttak);

        var dto = (SvpSak) tjeneste.hentSak(behandling.getFagsak());
        assertThat(dto.saksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(dto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dto.vedtak()).hasSize(1);
        assertThat(dto.avsluttet()).isFalse();
        var vedtak = dto.vedtak().stream().findFirst().orElseThrow();
        assertThat(vedtak.vedtakstidspunkt()).isEqualTo(vedtakstidspunkt);

        var familieHendelse = dto.familieHendelse();
        assertThat(familieHendelse.fødselsdato()).isNull();
        assertThat(familieHendelse.antallBarn()).isZero();
        assertThat(familieHendelse.termindato()).isEqualTo(termindato);
        assertThat(familieHendelse.omsorgsovertakelse()).isNull();

        assertThat(dto.søknader()).hasSize(1);
        var søknad = dto.søknader().stream().findFirst().get();
        assertThat(søknad.mottattTidspunkt()).isEqualTo(mottattDokument.getMottattTidspunkt());

        assertThat(søknad.tilrettelegginger()).hasSize(1);
        var dtoTl = søknad.tilrettelegginger().stream().findFirst().orElseThrow();
        assertThat(dtoTl.aktivitet().type()).isEqualTo(SvpSak.Aktivitet.Type.FRILANS);
        assertThat(dtoTl.behovFom()).isEqualTo(tl.getBehovForTilretteleggingFom());
        assertThat(dtoTl.risikoFaktorer()).isEqualTo(tl.getOpplysningerOmRisikofaktorer().orElse(null));
        assertThat(dtoTl.tiltak()).isEqualTo(tl.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        assertThat(dtoTl.perioder()).hasSize(1);
        var dtoPeriode = dtoTl.perioder().stream().findFirst().orElseThrow();
        assertThat(dtoPeriode.arbeidstidprosent()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getStillingsprosent());
        assertThat(dtoPeriode.fom()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getFomDato());
        assertThat(dtoPeriode.type()).isEqualTo(SvpSak.TilretteleggingType.DELVIS);

        assertThat(vedtak.arbeidsforhold()).hasSize(1);
        var arbeidsforholdUttak = vedtak.arbeidsforhold().stream().findFirst().orElseThrow();
        assertThat(arbeidsforholdUttak.aktivitet().type()).isEqualTo(SvpSak.Aktivitet.Type.FRILANS);
        assertThat(arbeidsforholdUttak.tiltak()).isEqualTo(tl.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        assertThat(arbeidsforholdUttak.risikoFaktorer()).isEqualTo(tl.getOpplysningerOmRisikofaktorer().orElse(null));
        assertThat(arbeidsforholdUttak.ikkeOppfyltÅrsak()).isNull();
        assertThat(arbeidsforholdUttak.svpPerioder()).hasSize(1);
        var svpPeriode = arbeidsforholdUttak.svpPerioder().stream().findFirst().orElseThrow();
        assertThat(svpPeriode.fom()).isEqualTo(uttakPeriode.getFom());
        assertThat(svpPeriode.tom()).isEqualTo(uttakPeriode.getTom());
        assertThat(svpPeriode.utbetalingsgrad()).isEqualTo(uttakPeriode.getUtbetalingsgrad().decimalValue());
        assertThat(svpPeriode.arbeidstidprosent()).isEqualTo(stillingsprosent);
        assertThat(svpPeriode.resultatÅrsak()).isEqualTo(SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_OVERGANG_FORELDREPENGER);
        assertThat(svpPeriode.tilretteleggingType()).isEqualTo(SvpSak.TilretteleggingType.DELVIS);
    }

    private Long avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        return repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
    }

}
