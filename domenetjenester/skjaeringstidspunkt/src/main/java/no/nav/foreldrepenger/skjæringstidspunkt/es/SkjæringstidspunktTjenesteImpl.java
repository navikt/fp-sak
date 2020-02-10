package no.nav.foreldrepenger.skjæringstidspunkt.es;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt.Builder;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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
        final Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);

        final LocalDate oppgittSkjæringstidspunkt = utledSkjæringstidspunktFraOppgitteData(familieHendelseAggregat);
        final Optional<LocalDate> bekreftetSkjæringstidspunkt = utledSkjæringstidspunktFraBekreftedeData(familieHendelseAggregat);

        if (endringTjeneste.erEndringIPerioden(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt.orElse(null))) {
            return bekreftetSkjæringstidspunkt.orElseThrow(IllegalStateException::new);
        }
        return oppgittSkjæringstidspunkt;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
        Builder builder = Skjæringstidspunkt.builder();

        Optional<LocalDate> bekreftetSkjæringstidspunkt = utledSkjæringstidspunktFraBekreftedeData(familieHendelseAggregat);
        if(bekreftetSkjæringstidspunkt.isPresent()) {
            builder.medUtledetSkjæringstidspunkt(bekreftetSkjæringstidspunkt.get());
        } else {
            LocalDate oppgittSkjæringstidspunkt = utledSkjæringstidspunktFraOppgitteData(familieHendelseAggregat);
            builder.medUtledetSkjæringstidspunkt(oppgittSkjæringstidspunkt);
        }
        return builder.build();
    }

}
