package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/**
 * Representerer et grunnlag av medlemskap opplysninger brukt i en Behandling. De ulike aggregatene (fra register, fra
 * vurdering, fra søker) kan gjenbrukes på tvers av Behandlinger, mens grunnlaget tilhører en Behandling.
 *
 */
@Entity(name = "MedlemskapBehandlingsgrunnlag")
@Table(name = "GR_MEDLEMSKAP")
public class MedlemskapBehandlingsgrunnlagEntitet extends BaseEntitet {

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MEDLEMSKAP")
    private Long id;

    @ManyToOne(cascade = { /* NONE - Aldri cascade til et selvstendig aggregat! */ }, fetch = FetchType.EAGER)
    @JoinColumn(name = "OPPGITT_ID", nullable = true, unique = true)
    private MedlemskapOppgittTilknytningEntitet oppgittTilknytning;

    @ManyToOne(cascade = { /* NONE - Aldri cascade til et selvstendig aggregat! */ }, fetch = FetchType.EAGER)
    @JoinColumn(name = "REGISTRERT_ID", nullable = true, unique = true)
    @ChangeTracked
    private MedlemskapRegistrertEntitet registerMedlemskap;

    @ManyToOne(cascade = { /* NONE - Aldri cascade til et selvstendig aggregat! */ }, fetch = FetchType.EAGER)
    @JoinColumn(name = "VURDERING_ID", nullable = true, unique = true)
    private VurdertMedlemskapEntitet vurderingMedlemskapSkjæringstidspunktet;

    @ManyToOne(cascade = { /* NONE - Aldri cascade til et selvstendig aggregat! */ }, fetch = FetchType.EAGER)
    @JoinColumn(name = "VURDERING_LOPENDE_ID", nullable = true, unique = true)
    private VurdertMedlemskapPeriodeEntitet vurderingLøpendeMedlemskap;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    MedlemskapBehandlingsgrunnlagEntitet() {
        // default tom entitet
    }

    MedlemskapBehandlingsgrunnlagEntitet(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
    }


    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MedlemskapBehandlingsgrunnlagEntitet that)) {
            return false;
        }

        return Objects.equals(this.behandlingId, that.behandlingId)
                && Objects.equals(this.registerMedlemskap, that.registerMedlemskap)
                && Objects.equals(this.oppgittTilknytning, that.oppgittTilknytning)
                && Objects.equals(this.vurderingMedlemskapSkjæringstidspunktet, that.vurderingMedlemskapSkjæringstidspunktet);
    }


    @Override
    public int hashCode() {
        return Objects.hash(this.behandlingId, this.registerMedlemskap, this.oppgittTilknytning, this.vurderingMedlemskapSkjæringstidspunktet);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + getId()
        + ", vurdertMedlemskap=" + this.vurderingMedlemskapSkjæringstidspunktet
        + ", oppgittTilknytning=" + this.oppgittTilknytning
        + ", registerMedlemskap=" + this.registerMedlemskap
        + ">";
    }

    /* eksponeres ikke public for andre. */
    Long getId() {
        return id;
    }

    MedlemskapOppgittTilknytningEntitet getOppgittTilknytning() {
        return oppgittTilknytning;
    }

    MedlemskapRegistrertEntitet getRegisterMedlemskap() {
        return registerMedlemskap;
    }

    Set<MedlemskapPerioderEntitet> getRegistertMedlemskapPerioder() {
        if (registerMedlemskap == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(registerMedlemskap.getMedlemskapPerioder());
    }

    @Deprecated
    VurdertMedlemskapEntitet getVurderingMedlemskapSkjæringstidspunktet() {
        return vurderingMedlemskapSkjæringstidspunktet;
    }

    @Deprecated
    VurdertMedlemskapPeriodeEntitet getVurderingLøpendeMedlemskap() {
        return vurderingLøpendeMedlemskap;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    MedlemskapAggregat tilAggregat() {
        return new MedlemskapAggregat(this.getRegistertMedlemskapPerioder(), this.getOppgittTilknytning());
    }

    private static MedlemskapBehandlingsgrunnlagEntitet kopierTidligerGrunnlag(
                                                                               Optional<MedlemskapBehandlingsgrunnlagEntitet> tidligereGrunnlagOpt, Long nyBehandlingId) {
        var nyttGrunnlag = new MedlemskapBehandlingsgrunnlagEntitet(nyBehandlingId);

        if (tidligereGrunnlagOpt.isPresent()) {
            var tidligereGrunnlag = tidligereGrunnlagOpt.get();
            nyttGrunnlag.oppgittTilknytning = tidligereGrunnlag.oppgittTilknytning;
            nyttGrunnlag.vurderingMedlemskapSkjæringstidspunktet = tidligereGrunnlag.vurderingMedlemskapSkjæringstidspunktet;
            nyttGrunnlag.registerMedlemskap = tidligereGrunnlag.registerMedlemskap;
        }
        return nyttGrunnlag;
    }

    static MedlemskapBehandlingsgrunnlagEntitet fra(Optional<MedlemskapBehandlingsgrunnlagEntitet> tidligereGrunnlagOpt, Long nyBehandlingId,
                                                    MedlemskapRegistrertEntitet nyeData) {
        var nyttGrunnlag = kopierTidligerGrunnlag(tidligereGrunnlagOpt, nyBehandlingId);
        nyttGrunnlag.registerMedlemskap = nyeData;
        return nyttGrunnlag;
    }

    static MedlemskapBehandlingsgrunnlagEntitet fra(Optional<MedlemskapBehandlingsgrunnlagEntitet> tidligereGrunnlagOpt, Long nyBehandlingId,
                                                    MedlemskapOppgittTilknytningEntitet nyeData) {
        var nyttGrunnlag = kopierTidligerGrunnlag(tidligereGrunnlagOpt, nyBehandlingId);
        nyttGrunnlag.oppgittTilknytning = nyeData;
        return nyttGrunnlag;
    }

    static MedlemskapBehandlingsgrunnlagEntitet fra(Optional<MedlemskapBehandlingsgrunnlagEntitet> eksisterendeGrunnlag, Long nyBehandlingId) {
        return kopierTidligerGrunnlag(eksisterendeGrunnlag, nyBehandlingId);
    }

    static MedlemskapBehandlingsgrunnlagEntitet forRevurdering(Optional<MedlemskapBehandlingsgrunnlagEntitet> eksisterendeGrunnlag, Long nyBehandlingId) {
        var nyttGrunnlag = new MedlemskapBehandlingsgrunnlagEntitet(nyBehandlingId);

        if (eksisterendeGrunnlag.isPresent()) {
            var tidligereGrunnlag = eksisterendeGrunnlag.get();
            nyttGrunnlag.oppgittTilknytning = tidligereGrunnlag.oppgittTilknytning;
        }
        return nyttGrunnlag;
    }
}
