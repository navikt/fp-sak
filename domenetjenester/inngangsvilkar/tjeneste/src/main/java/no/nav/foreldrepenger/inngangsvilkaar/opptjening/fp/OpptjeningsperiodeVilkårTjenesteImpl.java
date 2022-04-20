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
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.LovVersjoner;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;

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

        final var data = new OpptjeningsPeriode();
        var evaluation = new RegelFastsettOpptjeningsperiode().evaluer(grunnlag, data);

        var resultat = InngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSPERIODEVILKÅR, evaluation, grunnlag, data);
        return resultat;
    }

    private OpptjeningsperiodeGrunnlag opprettGrunnlag(BehandlingReferanse ref) {

        var behandlingId = ref.behandlingId();
        final var hendelseAggregat = familieHendelseRepository.hentAggregat(behandlingId);
        final var hendelse = hendelseAggregat.getGjeldendeVersjon();
        final var sammenhengendeUttak = ref.getSkjæringstidspunkt().kreverSammenhengendeUttak();
        final var førsteUttaksdato = ref.getSkjæringstidspunkt().getFørsteUttaksdato();
        final var lovversjon = ref.getSkjæringstidspunkt().utenMinsterett() ? LovVersjoner.KLASSISK : LovVersjoner.PROP15L2122;

        var fagsakÅrsak = finnFagsakÅrsak(hendelse);
        var søkerRolle =  finnFagsakSøkerRolle(ref);
        Optional<LocalDate> morsMaksdato = !sammenhengendeUttak ? Optional.empty() :
            ytelseMaksdatoTjeneste.beregnMorsMaksdato(ref.saksnummer(), ref.relasjonRolle());
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

        var grunnlag = new OpptjeningsperiodeGrunnlag(
            fagsakÅrsak,
            søkerRolle,
            førsteUttaksdato,
            hendelsedato,
            termindato.orElse(null),
            morsMaksdato.orElse(null),
            lovversjon
        );
        return grunnlag;
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
