package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.SoekerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class OpptjeningsperiodeVilkårTjenesteImpl implements OpptjeningsperiodeVilkårTjeneste {

    private InngangsvilkårOversetter inngangsvilkårOversetter;
    private Period antallMånederOpptjeningsperiode;
    private Period tidligsteUttakFørFødselPeriode;
    private FamilieHendelseRepository familieHendelseRepository;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;


    OpptjeningsperiodeVilkårTjenesteImpl() {
        // for CDI proxy
    }

    /**
     * @param opptjeningsPeriode - Opptjeningsperiode før skjæringstidspunkt
     * @param tidligsteUttakFørFødselPeriode - Tidligste lovlige oppstart av uttak av foreldrepenger før fødsel.
     */
    @Inject
    public OpptjeningsperiodeVilkårTjenesteImpl(InngangsvilkårOversetter inngangsvilkårOversetter,
                                                FamilieHendelseRepository familieHendelseRepository,
                                                YtelseMaksdatoTjeneste beregnMorsMaksdatoTjeneste,
                                                @KonfigVerdi(value = "fp.opptjeningsperiode.lengde", defaultVerdi = "P10M") Period opptjeningsPeriode,
                                                @KonfigVerdi(value = "fp.uttak.tidligst.før.fødsel", defaultVerdi = "P12W") Period tidligsteUttakFørFødselPeriode) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.antallMånederOpptjeningsperiode = opptjeningsPeriode;
        this.tidligsteUttakFørFødselPeriode = tidligsteUttakFørFødselPeriode;
        this.familieHendelseRepository = familieHendelseRepository;
        this.ytelseMaksdatoTjeneste = beregnMorsMaksdatoTjeneste;
    }

    @Override
    public VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, LocalDate førsteUttaksdato) {

        var grunnlag = opprettGrunnlag(behandlingReferanse, førsteUttaksdato);
        grunnlag.setPeriodeLengde(antallMånederOpptjeningsperiode);
        grunnlag.setTidligsteUttakFørFødselPeriode(tidligsteUttakFørFødselPeriode);

        final var data = new OpptjeningsPeriode();
        var evaluation = new RegelFastsettOpptjeningsperiode().evaluer(grunnlag, data);

        var resultat = inngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSPERIODEVILKÅR, evaluation, grunnlag);
        resultat.setEkstraVilkårresultat(data);
        return resultat;
    }

    private OpptjeningsperiodeGrunnlag opprettGrunnlag(BehandlingReferanse ref, LocalDate førsteUttaksdato) {

        var grunnlag = new OpptjeningsperiodeGrunnlag();

        var behandlingId = ref.getBehandlingId();
        final var hendelseAggregat = familieHendelseRepository.hentAggregat(behandlingId);
        final var hendelse = hendelseAggregat.getGjeldendeVersjon();

        grunnlag.setFagsakÅrsak(finnFagsakÅrsak(hendelse));
        grunnlag.setSøkerRolle(finnFagsakSøkerRolle(ref));
        if (grunnlag.getFagsakÅrsak() == null || grunnlag.getSøkerRolle() == null) {
            throw new IllegalArgumentException("Utvikler-feil: Finner ikke årsak/rolle for behandling:" + behandlingId);
        }

        ytelseMaksdatoTjeneste.beregnMorsMaksdato(ref.getSaksnummer(), ref.getRelasjonsRolleType()).ifPresent(grunnlag::setMorsMaksdato);
        if (grunnlag.getFagsakÅrsak().equals(FagsakÅrsak.FØDSEL)) {
            hendelseAggregat.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).ifPresent(grunnlag::setTerminDato);
            grunnlag.setHendelsesDato(hendelseAggregat.finnGjeldendeFødselsdato());
        } else {
            grunnlag.setHendelsesDato(hendelse.getSkjæringstidspunkt());

        }
        if (grunnlag.getHendelsesDato() == null) {
            throw new IllegalArgumentException("Utvikler-feil: Finner ikke hendelsesdato for behandling:" + behandlingId);
        }

        grunnlag.setFørsteUttaksDato(førsteUttaksdato);

        return grunnlag;
    }

    private SoekerRolle finnFagsakSøkerRolle(BehandlingReferanse ref) {
        var relasjonsRolleType = ref.getRelasjonsRolleType();
        if (RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            return SoekerRolle.MORA;
        }
        if (RelasjonsRolleType.UDEFINERT.equals(relasjonsRolleType) || RelasjonsRolleType.BARN.equals(relasjonsRolleType)) {
            return null;
        }
        return SoekerRolle.FARA;
    }

    private FagsakÅrsak finnFagsakÅrsak(FamilieHendelseEntitet familieHendelse) {
        final var type = familieHendelse.getType();
        if (familieHendelse.getGjelderFødsel()) {
            return FagsakÅrsak.FØDSEL;
        }
        if (FamilieHendelseType.ADOPSJON.equals(type)) {
            return FagsakÅrsak.ADOPSJON;
        }
        if (FamilieHendelseType.OMSORG.equals(type)) {
            return FagsakÅrsak.OMSORG;
        }
        return null;
    }
}
