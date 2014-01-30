package org.wordpress.android.ui.comments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.DateTimeUtils;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by nbradbury on 1/29/14.
 */
public class CommentAdapter extends BaseAdapter {
    protected static interface OnLoadMoreListener {
        public void onLoadMore();
    }

    protected static interface OnCheckedItemsChangeListener {
        public void onCheckedItemsChanged();
    }

    private LayoutInflater mInflater;
    private OnLoadMoreListener mOnLoadMoreListener;
    private OnCheckedItemsChangeListener mOnCheckedChangeListener;
    private CommentList mComments = new CommentList();
    private HashSet<Integer> mCheckedCommentPositions = new HashSet<Integer>();

    private int mStatusColorSpam;
    private int mStatusColorUnapproved;
    private int mAvatarSz;

    private boolean mEnableCheckBoxes;

    private String mStatusTextSpam;
    private String mStatusTextUnapproved;
    private String mAnonymous;

    private Drawable mDefaultAvatar;

    protected CommentAdapter(Context context,
                             OnLoadMoreListener onLoadMoreListener,
                             OnCheckedItemsChangeListener onChangeListener) {
        mInflater = LayoutInflater.from(context);

        mOnLoadMoreListener = onLoadMoreListener;
        mOnCheckedChangeListener = onChangeListener;

        mStatusColorSpam = Color.parseColor("#FF0000");
        mStatusColorUnapproved = Color.parseColor("#D54E21");
        mStatusTextSpam = context.getResources().getString(R.string.spam);
        mStatusTextUnapproved = context.getResources().getString(R.string.unapproved);
        mAnonymous = context.getString(R.string.anonymous);

        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mDefaultAvatar = context.getResources().getDrawable(R.drawable.placeholder);
    }

    @Override
    public int getCount() {
        return (mComments != null ? mComments.size() : 0);
    }

    @Override
    public Object getItem(int position) {
        return mComments.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    protected void clear() {
        if (mComments.size() > 0) {
            mComments.clear();
            notifyDataSetChanged();
        }
    }

    protected void clearCheckedComments() {
        if (mCheckedCommentPositions.size() > 0) {
            mCheckedCommentPositions.clear();
            notifyDataSetChanged();
            if (mOnCheckedChangeListener != null)
                mOnCheckedChangeListener.onCheckedItemsChanged();
        }
    }

    protected int getCheckedCommentCount() {
        return mCheckedCommentPositions.size();
    }

    protected CommentList getCheckedComments() {
        CommentList comments = new CommentList();

        Iterator it = mCheckedCommentPositions.iterator();
        while (it.hasNext()) {
            int position = (Integer) it.next();
            comments.add(mComments.get(position));
        }

        return comments;
    }

    protected boolean isItemChecked(int position) {
        return mCheckedCommentPositions.contains(position);
    }

    protected void setItemChecked(int position, boolean isChecked) {
        if (isItemChecked(position) == isChecked)
            return;

        if (isChecked) {
            mCheckedCommentPositions.add(position);
        } else {
            mCheckedCommentPositions.remove(position);
        }

        notifyDataSetChanged();

        if (mOnCheckedChangeListener != null)
            mOnCheckedChangeListener.onCheckedItemsChanged();
    }

    protected void toggleItemChecked(int position) {
        setItemChecked(position, !isItemChecked(position));
    }

    protected void setEnableCheckBoxes(boolean enable) {
        if (enable == mEnableCheckBoxes)
            return;

        mEnableCheckBoxes = enable;
        if (mEnableCheckBoxes) {
            notifyDataSetChanged();
        } else {
            clearCheckedComments();
        }
    }

    protected void replaceComments(final CommentList comments) {
        mComments.replaceComments(comments);
        notifyDataSetChanged();
    }

    protected void deleteComments(final CommentList comments) {
        mComments.deleteComments(comments);
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final Comment comment = mComments.get(position);
        final CommentEntryWrapper wrapper;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.comment_row, null);
            wrapper = new CommentEntryWrapper(convertView);
            convertView.setTag(wrapper);
        } else {
            wrapper = (CommentEntryWrapper) convertView.getTag();
        }

        wrapper.populateFrom(comment, position);

        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= getCount()-1)
            mOnLoadMoreListener.onLoadMore();

        return convertView;
    }

    class CommentEntryWrapper {
        private TextView txtName;
        private TextView txtComment;
        private TextView txtStatus;
        private TextView txtPostTitle;
        private TextView txtDate;
        private NetworkImageView imgAvatar;
        private View row;
        private CheckBox bulkCheck;

        CommentEntryWrapper(View row) {
            this.row = row;

            txtName = (TextView) row.findViewById(R.id.name);
            txtComment = (TextView) row.findViewById(R.id.comment);
            txtStatus = (TextView) row.findViewById(R.id.status);
            txtPostTitle = (TextView) row.findViewById(R.id.postTitle);
            txtDate = (TextView) row.findViewById(R.id.text_date);
            bulkCheck = (CheckBox) row.findViewById(R.id.bulkCheck);

            imgAvatar = (NetworkImageView) row.findViewById(R.id.avatar);
            imgAvatar.setDefaultImageResId(R.drawable.placeholder);
        }

        void populateFrom(Comment comment, final int position) {
            txtName.setText(comment.hasAuthorName() ? comment.getAuthorName() : mAnonymous);
            txtPostTitle.setText(comment.getPostTitle());
            txtComment.setText(comment.getUnescapedCommentText());
            txtDate.setText(DateTimeUtils.javaDateToTimeSpan(comment.getDatePublished()));

            row.setId(Integer.valueOf(comment.commentID));

            // status is only shown for comments that haven't been approved
            switch (comment.getStatusEnum()) {
                case SPAM :
                    txtStatus.setText(mStatusTextSpam);
                    txtStatus.setTextColor(mStatusColorSpam);
                    txtStatus.setVisibility(View.VISIBLE);
                    break;
                case UNAPPROVED:
                    txtStatus.setText(mStatusTextUnapproved);
                    txtStatus.setTextColor(mStatusColorUnapproved);
                    txtStatus.setVisibility(View.VISIBLE);
                    break;
                default :
                    txtStatus.setVisibility(View.GONE);
                    break;
            }

            bulkCheck.setVisibility(mEnableCheckBoxes ? View.VISIBLE : View.GONE);
            if (mEnableCheckBoxes) {
                bulkCheck.setChecked(mCheckedCommentPositions.contains(position));
                bulkCheck.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        if (bulkCheck.isChecked()) {
                            setItemChecked(position, true);
                        } else {
                            setItemChecked(position, false);
                        }
                    }
                });
            }

            String avatarUrl = comment.getAvatarForDisplay(mAvatarSz);
            if (!TextUtils.isEmpty(avatarUrl)) {
                imgAvatar.setImageUrl(avatarUrl, WordPress.imageLoader);
            } else {
                imgAvatar.setImageDrawable(mDefaultAvatar);
            }
        }
    }

    /*
     * load comments from local db
     */
    protected boolean loadComments() {
        int localBlogId = WordPress.currentBlog.getLocalTableBlogId();

        mComments = CommentTable.getCommentsForBlog(localBlogId);

        // pre-calc transient values so they're cached when used by getView()
        for (Comment comment: mComments) {
            comment.getDatePublished();
            comment.getUnescapedCommentText();
            comment.getAvatarForDisplay(mAvatarSz);
        }

        notifyDataSetChanged();

        return true;
    }
}
