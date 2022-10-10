package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfristdatoer;
import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.SøknadsperiodeFristTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class SøknadsperiodeFristTjenesteImpl implements SøknadsperiodeFristTjeneste  {

    private static final Period SENESTE_UTTAK_FØR_TERMIN = Period.ofWeeks(3);

    private FamilieHendelseRepository familieHendelseRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private SøknadRepository søknadRepository;

    SøknadsperiodeFristTjenesteImpl() {
        // CDI
    }

    @Inject
    public SøknadsperiodeFristTjenesteImpl(SvangerskapspengerRepository svangerskapspengerRepository,
                                           FamilieHendelseRepository familieHendelseRepository,
                                           SøknadRepository søknadRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public Optional<Søknadsfristdatoer> finnSøknadsfrist(Long behandlingId) {
        var tomFraTermin = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(SøknadsperiodeFristTjenesteImpl::utledTilretteleggingTomFraTermin);
        var tilretteleggingFom = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .flatMap(SøknadsperiodeFristTjenesteImpl::utledNettoSøknadsperiodeFomFraGrunnlag);

        var periode = tilretteleggingFom.map(fom -> new LocalDateInterval(fom, tomFraTermin.orElse(fom))).orElse(null);

        return Optional.of(finnSøknadsfrist(behandlingId, periode));
    }

    private Søknadsfristdatoer finnSøknadsfrist(Long behandlingId, LocalDateInterval søknadsperiode) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId);
        final var brukfrist = søknadsperiode != null ? Søknadsfrister.søknadsfristDagytelse(søknadsperiode.getFomDato()) : null;

        var builder = Søknadsfristdatoer.builder()
            .medSøknadGjelderPeriode(søknadsperiode)
            .medUtledetSøknadsfrist(brukfrist);
        søknad.ifPresent(s -> builder.medSøknadMottattDato(s.getMottattDato()));
        søknad.filter(s -> brukfrist != null && s.getMottattDato().isAfter(brukfrist))
            .ifPresent(s -> builder.medDagerOversittetFrist(DAYS.between(brukfrist, s.getMottattDato())));
        return builder.build();
    }

    public static Optional<LocalDate> utledNettoSøknadsperiodeFomFraGrunnlag(SvpGrunnlagEntitet grunnlag) {
        var tilrettelegginger = grunnlag.getOpprinneligeTilrettelegginger();
        return Optional.ofNullable(tilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .filter(SvpTilretteleggingEntitet::getSkalBrukes)
            .filter(t -> !Boolean.TRUE.equals(t.getKopiertFraTidligereBehandling()))
            .map(BeregnTilrettleggingsdato::tidligstTilretteleggingFraTilrettelegging)
            .min(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> utledBruttoSøknadsperiodeFomFraGrunnlag(SvpGrunnlagEntitet grunnlag) {
        return new TilretteleggingFilter(grunnlag).getAktuelleTilretteleggingerFiltrert().stream()
            .map(BeregnTilrettleggingsdato::tidligstTilretteleggingFraTilrettelegging)
            .min(Comparator.naturalOrder());
    }

    private static Optional<LocalDate> utledTilretteleggingTomFraTermin(FamilieHendelseEntitet familieHendelse) {
        var fh = Optional.ofNullable(familieHendelse).filter(FamilieHendelseEntitet::getGjelderFødsel);
        var termindatoMinusFFF = fh.flatMap(FamilieHendelseEntitet::getTerminbekreftelse)
            .map(TerminbekreftelseEntitet::getTermindato)
            .map(t -> t.minus(SENESTE_UTTAK_FØR_TERMIN));
        var fødselsdato = fh.flatMap(FamilieHendelseEntitet::getFødselsdato);
        if (termindatoMinusFFF.isPresent() && fødselsdato.filter(f -> f.isBefore(termindatoMinusFFF.get())).isPresent()) {
            return fødselsdato.map(f -> f.minusDays(1));
        }
        return termindatoMinusFFF
            .or(() -> fh.map(FamilieHendelseEntitet::getSkjæringstidspunkt).map(d -> d.minus(SENESTE_UTTAK_FØR_TERMIN)))
            .map(t -> t.minusDays(1));
    }

}
