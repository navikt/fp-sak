package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;

class SvpHelper {

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    public SvpHelper(BehandlingRepositoryProvider behandlingRepositoryProvider, SvangerskapspengerRepository svpRepository) {
        this.behandlingRepositoryProvider = behandlingRepositoryProvider;
        this.svangerskapspengerRepository = svpRepository;
    }

    Behandling lagreBehandling() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        return scenario.lagre(behandlingRepositoryProvider);
    }

    Behandling lagreRevurdering(Behandling behandling) {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        return scenario.lagre(behandlingRepositoryProvider);
    }

    void lagreTerminbekreftelse(LocalDate termindato, Long behandlingId, LocalDate... fødselsdatoer) {
        var familieHendelseBuilderSøknad = byggSøknad(behandlingId, termindato);
        behandlingRepositoryProvider.getFamilieHendelseRepository().lagreSøknadHendelse(behandlingId, familieHendelseBuilderSøknad);
        if (fødselsdatoer.length > 0) {
            var familieHendelseBuilderRegister = byggRegister(behandlingId, termindato, fødselsdatoer);
            behandlingRepositoryProvider.getFamilieHendelseRepository().lagreRegisterHendelse(behandlingId, familieHendelseBuilderRegister);
        }
    }

    void lagreIngenTilrettelegging(Behandling behandling, LocalDate jordmorsdato, LocalDate tidligstMottattDato) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medIngenTilrettelegging(jordmorsdato, tidligstMottattDato,
                    no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde.SØKNAD)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
                .medMottattTidspunkt(LocalDateTime.now())
                .medKopiertFraTidligereBehandling(false)
                .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
                .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    void lagreDelvisTilrettelegging(Behandling behandling, LocalDate jordmorsdato, LocalDate tilretteleggingFom, BigDecimal arbeidsprosent) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medDelvisTilrettelegging(tilretteleggingFom, arbeidsprosent, jordmorsdato,
                no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde.SØKNAD)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    private FamilieHendelseBuilder byggSøknad(Long behandlingId, LocalDate termindato) {
        var builder = behandlingRepositoryProvider.getFamilieHendelseRepository().opprettBuilderForSøknad(behandlingId);
        var terminBuilder = builder.getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(termindato.minusMonths(7));
        return builder.medTerminbekreftelse(terminBuilder);
    }

    private FamilieHendelseBuilder byggRegister(Long behandlingId, LocalDate termindato, LocalDate... fødselsdatoer) {
        var builder = behandlingRepositoryProvider.getFamilieHendelseRepository().opprettBuilderForRegister(behandlingId);
        var terminBuilder = builder.getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(termindato.minusMonths(7));
        Arrays.stream(fødselsdatoer).forEach(builder::leggTilBarn);
        return builder.medTerminbekreftelse(terminBuilder);
    }

}
