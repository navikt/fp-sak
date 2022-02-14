package no.nav.foreldrepenger.skjæringstidspunkt.es;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste, SkjæringstidspunktRegisterinnhentingTjeneste {

    private FamilieHendelseRepository familieGrunnlagRepository;
    private RegisterInnhentingIntervall endringTjeneste;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                          RegisterInnhentingIntervall endringTjeneste) {
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.endringTjeneste = endringTjeneste;
    }

    /**
     * Bestem skjæringstidspunkt ut fra bekreftede data
     */
    private Optional<LocalDate> utledSkjæringstidspunktFraBekreftedeData(Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat) {
        return familieHendelseAggregat.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt);
    }

    /**
     * Bestem skjæringstidspunkt ut fra oppgitte data
     */
    private LocalDate utledSkjæringstidspunktFraOppgitteData(Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat) {
        return familieHendelseAggregat.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(null);
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        final var familieHendelseAggregat = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);

        final var oppgittSkjæringstidspunkt = utledSkjæringstidspunktFraOppgitteData(familieHendelseAggregat);
        final var bekreftetSkjæringstidspunkt = utledSkjæringstidspunktFraBekreftedeData(familieHendelseAggregat);

        if (endringTjeneste.erEndringIPerioden(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt.orElse(null))) {
            return bekreftetSkjæringstidspunkt.orElseThrow(IllegalStateException::new);
        }
        return oppgittSkjæringstidspunkt;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var familieHendelseAggregat = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);

        var skjæringstidspunkt = utledSkjæringstidspunktFraBekreftedeData(familieHendelseAggregat)
            .orElseGet(() -> utledSkjæringstidspunktFraOppgitteData(familieHendelseAggregat));
        var gjelderFødsel = familieHendelseAggregat.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getGjelderFødsel).orElse(true);
        var ytelseIntervall = skjæringstidspunkt != null ? new LocalDateInterval(skjæringstidspunkt.minusWeeks(4), skjæringstidspunkt.plusWeeks(4)) : null;

        return Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medUtledetMedlemsintervall(ytelseIntervall)
            .medGjelderFødsel(gjelderFødsel)
            .build();
    }

}
