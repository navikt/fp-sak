package no.nav.foreldrepenger.behandling.steg.foreslåresultat.svp;

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
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
class ForeslåBehandlingsresultatTjenesteImpl implements no.nav.foreldrepenger.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste {

    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
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
            @FagsakYtelseTypeRef("SVP") RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles) {
        this.svangerskapspengerUttakResultatRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.avslagsårsakTjeneste = avslagsårsakTjeneste;
        this.revurderingBehandlingsresultatutlederFelles = revurderingBehandlingsresultatutlederFelles;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();

    }

    protected boolean minstEnGyldigUttaksPeriode(Behandlingsresultat behandlingsresultat) {
        Optional<SvangerskapspengerUttakResultatEntitet> uttakResultat = svangerskapspengerUttakResultatRepository
                .hentHvisEksisterer(behandlingsresultat.getBehandlingId());
        if (!uttakResultat.isPresent()) {
            return false;
        }
        for (var arbeidsforhold : uttakResultat.get().getUttaksResultatArbeidsforhold()) {
            for (var periode : arbeidsforhold.getPerioder()) {
                if (periode.isInnvilget()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.getBehandlingId());
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
        return dokumentBehandlingTjeneste.erDokumentBestilt(ref.getBehandlingId(), DokumentMalType.REVURDERING_DOK)
            || dokumentBehandlingTjeneste.erDokumentBestilt(ref.getBehandlingId(), DokumentMalType.VARSEL_OM_REVURDERING);
    }
}
