package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import java.time.LocalDate;
import java.util.Optional;

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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.LovVersjoner;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class OpptjeningsperiodeVilkårTjenesteImpl implements OpptjeningsperiodeVilkårTjeneste {

    private FamilieHendelseRepository familieHendelseRepository;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;


    OpptjeningsperiodeVilkårTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningsperiodeVilkårTjenesteImpl(FamilieHendelseRepository familieHendelseRepository,
                                                YtelseMaksdatoTjeneste beregnMorsMaksdatoTjeneste) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.ytelseMaksdatoTjeneste = beregnMorsMaksdatoTjeneste;
    }

    @Override
    public VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse) {

        var grunnlag = opprettGrunnlag(behandlingReferanse);

        var resultat = InngangsvilkårRegler.opptjeningsperiode(RegelYtelse.FORELDREPENGER, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSPERIODEVILKÅR, resultat);
    }

    private OpptjeningsperiodeGrunnlag opprettGrunnlag(BehandlingReferanse ref) {

        var behandlingId = ref.behandlingId();
        var hendelseAggregat = familieHendelseRepository.hentAggregat(behandlingId);
        var hendelse = hendelseAggregat.getGjeldendeVersjon();
        var sammenhengendeUttak = ref.getSkjæringstidspunkt().kreverSammenhengendeUttak();
        var førsteUttaksdato = ref.getSkjæringstidspunkt().getFørsteUttaksdato();
        var lovversjon = ref.getSkjæringstidspunkt().utenMinsterett() ? LovVersjoner.KLASSISK : LovVersjoner.PROP15L2122;

        var fagsakÅrsak = finnFagsakÅrsak(hendelse);
        var søkerRolle = finnFagsakSøkerRolle(ref);
        Optional<LocalDate> morsMaksdato = !sammenhengendeUttak ? Optional.empty() : ytelseMaksdatoTjeneste.beregnMorsMaksdato(ref.saksnummer(),
            ref.relasjonRolle());
        Optional<LocalDate> termindato;
        LocalDate hendelsedato;
        if (FagsakÅrsak.FØDSEL.equals(fagsakÅrsak)) {
            termindato = hendelseAggregat.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
            hendelsedato = hendelseAggregat.finnGjeldendeFødselsdato();
        } else {
            termindato = Optional.empty();
            hendelsedato = hendelse.getSkjæringstidspunkt();
        }

        if (fagsakÅrsak == null || søkerRolle == null) {
            throw new IllegalArgumentException("Utvikler-feil: Finner ikke årsak/rolle for behandling:" + behandlingId);
        }
        if (hendelsedato == null) {
            throw new IllegalArgumentException("Utvikler-feil: Finner ikke hendelsesdato for behandling:" + behandlingId);
        }

        return OpptjeningsperiodeGrunnlag.grunnlag(fagsakÅrsak, søkerRolle, lovversjon)
            .medFørsteUttaksDato(førsteUttaksdato)
            .medHendelsesDato(hendelsedato)
            .medTerminDato(termindato.orElse(null))
            .medMorsMaksdato(morsMaksdato.orElse(null));
    }

    private RegelSøkerRolle finnFagsakSøkerRolle(BehandlingReferanse ref) {
        var relasjonsRolleType = ref.relasjonRolle();
        if (RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            return RegelSøkerRolle.MORA;
        }
        if (RelasjonsRolleType.UDEFINERT.equals(relasjonsRolleType) || RelasjonsRolleType.BARN.equals(relasjonsRolleType)) {
            return null;
        }
        return RegelSøkerRolle.FARA;
    }

    private FagsakÅrsak finnFagsakÅrsak(FamilieHendelseEntitet familieHendelse) {
        var type = familieHendelse.getType();
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
