package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class MedlemskapAksjonspunktFellesTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private VilkårMedlemskapRepository vilkårMedlemskapRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public MedlemskapAksjonspunktFellesTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                VilkårMedlemskapRepository vilkårMedlemskapRepository,
                                                VilkårResultatRepository vilkårResultatRepository) {
        this.historikkAdapter = historikkAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.vilkårMedlemskapRepository = vilkårMedlemskapRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    MedlemskapAksjonspunktFellesTjeneste() {
        //CDI
    }

    public VilkårUtfallType oppdater(long behandlingId,
                                     Avslagsårsak avslagsårsak,
                                     LocalDate opphørFom,
                                     String begrunnelse,
                                     SkjermlenkeType skjermlenkeType) {
        if (avslagsårsak != null && !VilkårType.MEDLEMSKAPSVILKÅRET.getAvslagsårsaker().contains(avslagsårsak)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for medlemskapsvilkåret");
        }
        var opphørEtterStp = erOpphørEtterStp(behandlingId, opphørFom);
        var utfall = avslagsårsak == null || opphørEtterStp ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslag(utfall, begrunnelse, opphørFom, skjermlenkeType);

        var vilkårResultat = vilkårResultatRepository.hent(behandlingId);
        vilkårMedlemskapRepository.slettFor(vilkårResultat);
        if (opphørEtterStp) {
            vilkårMedlemskapRepository.lagre(VilkårMedlemskap.forOpphør(vilkårResultat, opphørFom, avslagsårsak));
        }

        return utfall;
    }

    public VilkårUtfallType oppdaterForutgående(long behandlingId,
                                                Avslagsårsak avslagsårsak,
                                                LocalDate medlemFom,
                                                String begrunnelse,
                                                SkjermlenkeType skjermlenkeType) {
        if (avslagsårsak != null && !VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE.getAvslagsårsaker().contains(avslagsårsak)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for medlemskapsvilkåret");
        }
        var utfall = avslagsårsak == null ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslagForutgående(utfall, begrunnelse, medlemFom, skjermlenkeType);

        var vilkårResultat = vilkårResultatRepository.hent(behandlingId);
        vilkårMedlemskapRepository.slettFor(vilkårResultat);
        if (utfall == VilkårUtfallType.IKKE_OPPFYLT && medlemFom != null) {
            vilkårMedlemskapRepository.lagre(VilkårMedlemskap.forMedlemFom(vilkårResultat, medlemFom));
        }

        return utfall;
    }

    private void lagHistorikkInnslag(VilkårUtfallType nyVerdi, String begrunnelse, LocalDate opphørFom, SkjermlenkeType skjermlenkeType) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET, null, nyVerdi)
            .medBegrunnelse(begrunnelse).medSkjermlenke(skjermlenkeType);

        if (nyVerdi.equals(VilkårUtfallType.OPPFYLT) && opphørFom != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET_OPPHØRSDATO, null, opphørFom);
        }
    }

    private void lagHistorikkInnslagForutgående(VilkårUtfallType nyVerdi, String begrunnelse, LocalDate medlemFom, SkjermlenkeType skjermlenkeType) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET, null, nyVerdi)
            .medBegrunnelse(begrunnelse).medSkjermlenke(skjermlenkeType);

        if (VilkårUtfallType.IKKE_OPPFYLT.equals(nyVerdi) && medlemFom != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET_MEDLEMFRADATO, null, medlemFom);
        }
    }

    private boolean erOpphørEtterStp(Long behandlingId, LocalDate opphørFom) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        return opphørFom != null && opphørFom.isAfter(stp);
    }

}
