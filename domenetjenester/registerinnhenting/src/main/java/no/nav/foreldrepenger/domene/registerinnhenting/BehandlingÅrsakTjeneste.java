package no.nav.foreldrepenger.domene.registerinnhenting;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak.EndringResultatType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
public class BehandlingÅrsakTjeneste {

    private Instance<BehandlingÅrsakUtleder> utledere;
    private EndringsresultatSjekker endringsresultatSjekker;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public BehandlingÅrsakTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public BehandlingÅrsakTjeneste(@Any Instance<BehandlingÅrsakUtleder> utledere,
                                   EndringsresultatSjekker endringsresultatSjekker,
                                   RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.utledere = utledere;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void lagHistorikkForRegisterEndringerMotOriginalBehandling(Behandling revurdering) {
        var origBehandling = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        var snapshotOrig = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(origBehandling);
        var diff = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(revurdering.getId(), snapshotOrig);

        var endringsTyper = utledEndringsResultatTyperBasertPåDiff(revurdering, diff);
        lagHistorikkinnslag(revurdering, endringsTyper, false);
    }

    public void lagHistorikkForRegisterEndringsResultat(Behandling behandling, EndringsresultatDiff endringsresultatDiff) {
        var endringsTyper = utledEndringsResultatTyperBasertPåDiff(behandling, endringsresultatDiff);
        lagHistorikkinnslag(behandling, endringsTyper, true);
    }

    private void lagHistorikkinnslag(Behandling behandling, Set<EndringResultatType> endringsTyper, boolean medRegisterInnslag) {
        if (endringsTyper.contains(EndringResultatType.OPPLYSNING_OM_DØD)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD);
            return;
        }
        if (endringsTyper.contains(EndringResultatType.OPPLYSNING_OM_YTELSER)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER);
            return;
        }
        if (medRegisterInnslag && endringsTyper.contains(EndringResultatType.REGISTEROPPLYSNING)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForNyeRegisteropplysninger(behandling);
        }
    }

    private Set<EndringResultatType> utledEndringsResultatTyperBasertPåDiff(Behandling behandling, EndringsresultatDiff endringsresultatDiff) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return endringsresultatDiff.erSporedeFeltEndret() ? Collections.singleton(EndringResultatType.REGISTEROPPLYSNING) : Collections.emptySet();
        }
        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        //For alle aggregat som har endringer, utled behandlingsårsak.
        return endringsresultatDiff.hentDelresultater().stream()
            .filter(EndringsresultatDiff::erSporedeFeltEndret)
            .map(diff -> finnUtleder(diff.getGrunnlag())
                .utledEndringsResultat(ref, stp, diff.getGrunnlagId1(), diff.getGrunnlagId2()))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private BehandlingÅrsakUtleder finnUtleder(Class<?> aggregat) {
        var aggrNavn = aggregat.getSimpleName();
        if(aggregat.isAnnotationPresent(Entity.class)) {
            aggrNavn = aggregat.getAnnotation(Entity.class).name();
        }

        var selected = utledere.select(new GrunnlagRef.GrunnlagRefLiteral(aggrNavn));
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for BehandlingÅrsakUtleder:" + aggrNavn);
        }
        if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for BehandlingÅrsakUtleder:" + aggrNavn);
        }
        var minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return selected.get();
    }
}
