package no.nav.foreldrepenger.behandling.steg.foreslåresultat.fp;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandling.steg.foreslåresultat.AvslagsårsakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
class ForeslåBehandlingsresultatTjenesteImpl implements no.nav.foreldrepenger.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste {

    private UttakRepository uttakRepository;

    private AvslagsårsakTjeneste avslagsårsakTjeneste;

    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private FagsakRepository fagsakRepository;

    ForeslåBehandlingsresultatTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåBehandlingsresultatTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                         AvslagsårsakTjeneste avslagsårsakTjeneste,
                                         DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                         @FagsakYtelseTypeRef("FP") RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles) {
        this.uttakRepository =repositoryProvider.getUttakRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.avslagsårsakTjeneste =avslagsårsakTjeneste;
        this.revurderingBehandlingsresultatutlederFelles =revurderingBehandlingsresultatutlederFelles;
        this.dokumentBehandlingTjeneste =dokumentBehandlingTjeneste;
        this.behandlingsresultatRepository =repositoryProvider.getBehandlingsresultatRepository();
    }


    protected boolean minstEnGyldigUttaksPeriode(Behandlingsresultat behandlingsresultat) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        return uttakResultat.isPresent() && uttakResultat.get().getGjeldendePerioder().getPerioder().stream().anyMatch(UttakResultatPeriodeEntitet::isInnvilget);
    }


    @Override
    public Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        if (behandlingsresultat.isPresent()) {
            if (sjekkVilkårAvslått(behandlingsresultat.get())) {
                vilkårAvslått(ref, behandlingsresultat.get());
            } else {
                Behandlingsresultat.builderEndreEksisterende(behandlingsresultat.get()).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
                // Må nullstille avslagårsak (for symmetri med setting avslagsårsak ovenfor, hvor avslagårsak kopieres fra et vilkår)
                Optional.ofNullable(behandlingsresultat.get().getAvslagsårsak()).ifPresent(ufjernetÅrsak -> behandlingsresultat.get().setAvslagsårsak(Avslagsårsak.UDEFINERT));
                if (ref.erRevurdering()) {
                    boolean erVarselOmRevurderingSendt = erVarselOmRevurderingSendt(ref);
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
        Optional<Vilkår> ikkeOppfyltVilkår = behandlingsresultat.getVilkårResultat().hentIkkeOppfyltVilkår();
        ikkeOppfyltVilkår.ifPresent(vilkår -> {
            Avslagsårsak avslagsårsak = avslagsårsakTjeneste.finnAvslagsårsak(vilkår);
            behandlingsresultat.setAvslagsårsak(avslagsårsak);
        });
        if (ref.erRevurdering()) {
            boolean erVarselOmRevurderingSendt = erVarselOmRevurderingSendt(ref);
            revurderingBehandlingsresultatutlederFelles.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
        } else {
            Behandlingsresultat.Builder resultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            if (skalTilInfoTrygd(ref)) {
                resultatBuilder.medVedtaksbrev(Vedtaksbrev.INGEN);
            }
        }
    }

    private boolean skalTilInfoTrygd(BehandlingReferanse ref) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(ref.getSaksnummer()).orElseThrow();
        return fagsak.getSkalTilInfotrygd();
    }

    private boolean erVarselOmRevurderingSendt(BehandlingReferanse ref) {
        return dokumentBehandlingTjeneste.erDokumentProdusert(ref.getBehandlingId(), DokumentMalType.REVURDERING_DOK);
    }
}
