package no.nav.foreldrepenger.behandling.steg.foreslåresultat.fp;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.AvslagsårsakMapper;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
class ForeslåBehandlingsresultatTjenesteImpl implements ForeslåBehandlingsresultatTjeneste {

    private ForeldrepengerUttakTjeneste uttakTjeneste;

    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private FagsakRepository fagsakRepository;

    ForeslåBehandlingsresultatTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåBehandlingsresultatTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
            ForeldrepengerUttakTjeneste uttakTjeneste,
            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
            @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles) {
        this.uttakTjeneste = uttakTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.revurderingBehandlingsresultatutlederFelles = revurderingBehandlingsresultatutlederFelles;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    protected boolean minstEnGyldigUttaksPeriode(Behandlingsresultat behandlingsresultat) {
        var uttak = uttakTjeneste.hentUttakHvisEksisterer(behandlingsresultat.getBehandlingId());
        return uttak.isPresent() && uttak.get().getGjeldendePerioder().stream().anyMatch(ForeldrepengerUttakPeriode::isInnvilget);
    }

    @Override
    public Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.behandlingId());
        if (behandlingsresultat.isPresent()) {
            if (sjekkVilkårAvslått(behandlingsresultat.get())) {
                vilkårAvslått(ref, behandlingsresultat.get());
            } else {
                Behandlingsresultat.builderEndreEksisterende(behandlingsresultat.get()).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
                // Må nullstille avslagårsak (for symmetri med setting avslagsårsak ovenfor,
                // hvor avslagårsak kopieres fra et vilkår)
                Optional.ofNullable(behandlingsresultat.get().getAvslagsårsak())
                        .ifPresent(ufjernetÅrsak -> behandlingsresultat.get().setAvslagsårsak(Avslagsårsak.UDEFINERT));
                if (ref.erRevurdering()) {
                    var erVarselOmRevurderingSendt = erVarselOmRevurderingSendt(ref);
                    return revurderingBehandlingsresultatutlederFelles.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
                }
            }
        }
        return behandlingsresultat.orElse(null);
    }

    private boolean sjekkVilkårAvslått(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.isVilkårAvslått() || !minstEnGyldigUttaksPeriode(behandlingsresultat);
    }

    private void vilkårAvslått(BehandlingReferanse ref, Behandlingsresultat behandlingsresultat) {
        behandlingsresultat.getVilkårResultat().hentIkkeOppfyltVilkår()
            .map(AvslagsårsakMapper::finnAvslagsårsak)
            .ifPresent(behandlingsresultat::setAvslagsårsak);
        if (ref.erRevurdering()) {
            var erVarselOmRevurderingSendt = erVarselOmRevurderingSendt(ref);
            revurderingBehandlingsresultatutlederFelles.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
        } else {
            var resultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                    .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            if (sakErStengt(ref)) {
                resultatBuilder.medVedtaksbrev(Vedtaksbrev.INGEN);
            }
        }
    }

    private boolean sakErStengt(BehandlingReferanse ref) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(ref.saksnummer()).orElseThrow();
        return fagsak.erStengt();
    }

    private boolean erVarselOmRevurderingSendt(BehandlingReferanse ref) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(ref.behandlingId(), DokumentMalType.VARSEL_OM_REVURDERING);
    }
}
