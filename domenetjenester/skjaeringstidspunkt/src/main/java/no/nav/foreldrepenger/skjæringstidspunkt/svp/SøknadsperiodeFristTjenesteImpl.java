package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
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
        var familieHendelseEntitet = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        var tilretteleggingFom = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .flatMap(SøknadsperiodeFristTjenesteImpl::utledNettoSøknadsperiodeFomFraGrunnlag);

        if (fødselFørTilrettelegging(familieHendelseEntitet, tilretteleggingFom)) {
            return Optional.of(finnSøknadsfrist(behandlingId, null));
        }

        var tomFraTermin = familieHendelseEntitet
            .flatMap(SøknadsperiodeFristTjenesteImpl::utledTilretteleggingTomFraTermin);

        var periode = tilretteleggingFom.map(fom -> new LocalDateInterval(fom, tomFraTermin.orElse(fom))).orElse(null);

        return Optional.of(finnSøknadsfrist(behandlingId, periode));
    }

    private boolean fødselFørTilrettelegging(Optional<FamilieHendelseEntitet> familieHendelseEntitet, Optional<LocalDate> tilretteleggingFom) {
        return familieHendelseEntitet.isPresent() && tilretteleggingFom.isPresent() && familieHendelseEntitet.get()
            .getFødselsdato()
            .filter(fødselsdato -> fødselsdato.isBefore(tilretteleggingFom.get()))
            .isPresent();
    }

    private Søknadsfristdatoer finnSøknadsfrist(Long behandlingId, LocalDateInterval søknadsperiode) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId);
        var brukfrist = søknadsperiode != null ? Søknadsfrister.søknadsfristDagytelse(søknadsperiode.getFomDato()) : null;

        var builder = Søknadsfristdatoer.builder()
            .medSøknadGjelderPeriode(søknadsperiode)
            .medUtledetSøknadsfrist(brukfrist);
        søknad.ifPresent(s -> builder.medSøknadMottattDato(s.getMottattDato()));
        søknad.filter(s -> brukfrist != null && s.getMottattDato().isAfter(brukfrist))
            .ifPresent(s -> builder.medDagerOversittetFrist(DAYS.between(brukfrist, s.getMottattDato())));
        return builder.build();
    }

    public static Optional<LocalDate> utledNettoSøknadsperiodeFomFraGrunnlag(SvpGrunnlagEntitet grunnlag) {
        var tilrettelegginger = grunnlag.getGjeldendeVersjon();
        return Optional.ofNullable(tilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .filter(SvpTilretteleggingEntitet::getSkalBrukes)
            .map(BeregnTilrettleggingsdato::tidligstTilretteleggingFraTilrettelegging)
            .min(Comparator.naturalOrder());
    }

    public static Optional<DatoerSøknadsfrist> utledFørsteSøknadsperiodeFomFraGrunnlag(SvpGrunnlagEntitet grunnlag) {
        var tilrettelegginger = grunnlag.getGjeldendeVersjon();
        return Optional.of(tilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .filter(SvpTilretteleggingEntitet::getSkalBrukes)
            .map(SøknadsperiodeFristTjenesteImpl::hentTidligsteFraDato)
            .flatMap(Optional::stream)
            .min(Comparator.comparing(TilretteleggingFOM::getFomDato))
            .map(d -> new DatoerSøknadsfrist(d.getFomDato(), d.getTidligstMotattDato()));

    }

    private static Optional<TilretteleggingFOM> hentTidligsteFraDato(SvpTilretteleggingEntitet tilrettelegging) {
        var helTilrettelegging = tilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(tl -> tl.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING))
            .min(Comparator.comparing(TilretteleggingFOM::getFomDato));
        var delvisTilrettelegging = tilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(tl -> tl.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING))
            .min(Comparator.comparing(TilretteleggingFOM::getFomDato));
        var slutteArbeid = tilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(tl -> tl.getType().equals(TilretteleggingType.INGEN_TILRETTELEGGING))
            .min(Comparator.comparing(TilretteleggingFOM::getFomDato));

        return  Stream.of(helTilrettelegging, delvisTilrettelegging, slutteArbeid)
            .flatMap(Optional::stream)
            .min(Comparator.comparing(TilretteleggingFOM::getFomDato));
    }

    public record DatoerSøknadsfrist(LocalDate førsteUttakDato, LocalDate tidligstMottatt ) {}

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
