package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class MedlemskapAksjonspunktFellesTjeneste {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private VilkårMedlemskapRepository vilkårMedlemskapRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Historikkinnslag2Repository historikkRepository;

    @Inject
    public MedlemskapAksjonspunktFellesTjeneste(SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                VilkårMedlemskapRepository vilkårMedlemskapRepository,
                                                VilkårResultatRepository vilkårResultatRepository,
                                                Historikkinnslag2Repository historikkRepository) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.vilkårMedlemskapRepository = vilkårMedlemskapRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.historikkRepository = historikkRepository;
    }

    MedlemskapAksjonspunktFellesTjeneste() {
        //CDI
    }

    public VilkårUtfallType oppdater(BehandlingReferanse ref,
                                     Avslagsårsak avslagsårsak,
                                     LocalDate opphørFom,
                                     String begrunnelse,
                                     SkjermlenkeType skjermlenkeType) {
        if (avslagsårsak != null && !VilkårType.MEDLEMSKAPSVILKÅRET.getAvslagsårsaker().contains(avslagsårsak)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for medlemskapsvilkåret");
        }
        var opphørEtterStp = erOpphørEtterStp(ref, opphørFom);
        var utfall = avslagsårsak == null || opphørEtterStp ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslag(ref, utfall, begrunnelse, opphørFom, skjermlenkeType);

        var vilkårResultat = vilkårResultatRepository.hent(ref.behandlingId());
        vilkårMedlemskapRepository.slettFor(vilkårResultat);
        if (opphørEtterStp) {
            vilkårMedlemskapRepository.lagre(VilkårMedlemskap.forOpphør(vilkårResultat, opphørFom, avslagsårsak));
        }

        return utfall;
    }

    public VilkårUtfallType oppdaterForutgående(BehandlingReferanse ref,
                                                Avslagsårsak avslagsårsak,
                                                LocalDate medlemFom,
                                                String begrunnelse,
                                                SkjermlenkeType skjermlenkeType) {
        if (avslagsårsak != null && !VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE.getAvslagsårsaker().contains(avslagsårsak)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for medlemskapsvilkåret");
        }
        var utfall = avslagsårsak == null ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslagForutgående(ref, utfall, begrunnelse, medlemFom, skjermlenkeType);

        var vilkårResultat = vilkårResultatRepository.hent(ref.behandlingId());
        vilkårMedlemskapRepository.slettFor(vilkårResultat);
        if (utfall == VilkårUtfallType.IKKE_OPPFYLT && medlemFom != null) {
            vilkårMedlemskapRepository.lagre(VilkårMedlemskap.forMedlemFom(vilkårResultat, medlemFom));
        }

        return utfall;
    }

    private void lagHistorikkInnslag(BehandlingReferanse ref,
                                     VilkårUtfallType nyVerdi,
                                     String begrunnelse,
                                     LocalDate opphørFom,
                                     SkjermlenkeType skjermlenkeType) {
        var builder = lagFellesHistorikkBuilder(ref, nyVerdi, skjermlenkeType);

        if (nyVerdi.equals(VilkårUtfallType.OPPFYLT) && opphørFom != null) {
            builder.addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().til("Opphørt medlemskap", opphørFom));
        }
        builder.addTekstlinje(begrunnelse);
        historikkRepository.lagre(builder.build());
    }

    private void lagHistorikkInnslagForutgående(BehandlingReferanse ref,
                                                VilkårUtfallType nyVerdi,
                                                String begrunnelse,
                                                LocalDate medlemFom,
                                                SkjermlenkeType skjermlenkeType) {

        var builder = lagFellesHistorikkBuilder(ref, nyVerdi, skjermlenkeType);

        if (VilkårUtfallType.IKKE_OPPFYLT.equals(nyVerdi) && medlemFom != null) {
            builder.addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().til("Innflyttingsdato", medlemFom));
        }
        builder.addTekstlinje(begrunnelse);
        historikkRepository.lagre(builder.build());
    }

    private static Historikkinnslag2.Builder lagFellesHistorikkBuilder(BehandlingReferanse ref,
                                                                       VilkårUtfallType nyVerdi,
                                                                       SkjermlenkeType skjermlenkeType) {
        return new Historikkinnslag2.Builder().medBehandlingId(ref.behandlingId())
            .medFagsakId(ref.fagsakId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(skjermlenkeType)
            .addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().til("Medlemskap", nyVerdi.getNavn().toLowerCase()));
    }

    private boolean erOpphørEtterStp(BehandlingReferanse ref, LocalDate opphørFom) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId()).getUtledetSkjæringstidspunkt();
        return opphørFom != null && opphørFom.isAfter(stp);
    }

}
