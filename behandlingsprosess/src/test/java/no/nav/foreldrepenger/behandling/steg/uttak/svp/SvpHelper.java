package no.nav.foreldrepenger.behandling.steg.uttak.svp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.vedtak.util.FPDateUtil;

class SvpHelper {

    private BehandlingRepositoryProvider behandlingRepositoryProvider;

    public SvpHelper(BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.behandlingRepositoryProvider = behandlingRepositoryProvider;
    }

    Behandling lagreBehandling() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        return scenario.lagre(behandlingRepositoryProvider);
    }

    void lagreTerminbekreftelse(Behandling behandling, LocalDate termindato, LocalDate... fødselsdatoer) {
        var familieHendelseBuilder = byggAggregat(termindato, fødselsdatoer);
        behandlingRepositoryProvider.getFamilieHendelseRepository().lagre(behandling, familieHendelseBuilder);
    }

    void lagreIngenTilrettelegging(Behandling behandling, LocalDate jordmorsdato) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medIngenTilrettelegging(jordmorsdato)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(FPDateUtil.nå())
            .medKopiertFraTidligereBehandling(false)
            .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        behandlingRepositoryProvider.getSvangerskapspengerRepository().lagreOgFlush(svpGrunnlag);
    }

    private FamilieHendelseBuilder byggAggregat(LocalDate termindato, LocalDate... fødselsdatoer) {
        var builder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET);
        var terminBuilder = builder.getTerminbekreftelseBuilder();
        terminBuilder.medTermindato(termindato);
        terminBuilder.medUtstedtDato(termindato.minusMonths(7));
        for (LocalDate fødselsdato : fødselsdatoer) {
            builder.leggTilBarn(fødselsdato);
        }
        return builder.medTerminbekreftelse(terminBuilder);
    }

}
